package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String staffUrl = "https://cs.pollub.pl/staff";
        String newsUrl = "https://weii.pollub.pl/aktualnosci/filtr,studenci,1,page";
//        String newsUrl = "https://weii.pollub.pl/aktualnosci/page";

        try {
            // Pobieranie danych o pracownikach
            Document staffDoc = Jsoup.connect(staffUrl).get();
            Elements departments = staffDoc.select("h3");
            Map<String, List<String>> staffByDepartment = new LinkedHashMap<>();
            Map<String, List<String>> filteredStaffByDepartment = new LinkedHashMap<>();

            for (Element department : departments) {
                String departmentName = department.text();
                Element nextSibling = department.nextElementSibling();
                List<String> staffList = new ArrayList<>();
                List<String> filteredStaffList = new ArrayList<>();

                while (nextSibling != null && nextSibling.tagName().equals("p")) {
                    Elements links = nextSibling.select("a");
                    for (Element link : links) {
                        String staffName = link.text();
                        staffList.add(staffName);
                        if (FilterStuff(staffName)) {
                            filteredStaffList.add(staffName);
                        }
                    }
                    nextSibling = nextSibling.nextElementSibling();
                }

                // Sortowanie listy pracowników
                staffList.sort(String::compareToIgnoreCase);
                filteredStaffList.sort(String::compareToIgnoreCase);

                staffByDepartment.put(departmentName, staffList);
                filteredStaffByDepartment.put(departmentName, filteredStaffList);
            }

            // Wypisywanie wszystkich pracowników
            System.out.println("Wszyscy pracownicy:");
            for (Map.Entry<String, List<String>> entry : staffByDepartment.entrySet()) {
                System.out.println(entry.getKey());
                for (String staff : entry.getValue()) {
                    System.out.println(" - " + staff);
                }
            }

            // Wypisywanie pracowników z tytułem "dr" lub "dr inż."
            System.out.println("\nPracownicy z tytułem 'dr' lub 'dr inż.':");
            for (Map.Entry<String, List<String>> entry : filteredStaffByDepartment.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    System.out.println(entry.getKey());
                    for (String staff : entry.getValue()) {
                        System.out.println(" - " + staff);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Błąd podczas pobierania danych: " + e.getMessage());
        }



        // * ZAD-10-3
        try {
            Document firstPageDoc = Jsoup.connect(newsUrl + "1.html").get();

            Elements paginationLinks = firstPageDoc.select("ul.pagination li.page-item a.page-link");

            List<FreeDayEvent> freeDayEvents = new ArrayList<>();

            int lastPage = 1;
            for (Element link : paginationLinks) {
                String title = link.attr("title");

                try {
                    int pageNumber = Integer.parseInt(title.trim());
                    if (pageNumber > lastPage) {
                        lastPage = pageNumber;
                    }
                } catch (NumberFormatException e) {}
            }

//            System.out.println("Ostatnia strona to: " + lastPage);
            for (int page = 1; page <= lastPage; page++) {
                // Tworzenie pełnego URL
                String currentUrl = newsUrl + page + ".html";
                Document newsDoc = Jsoup.connect(currentUrl).get();

                // Wyszukiwanie artykułów
                Elements newsContainers = newsDoc.select("div.row.mt-4 .column");

                if (newsContainers.isEmpty()) {
                    break;
                }

                for (Element container : newsContainers) {
                    Element titleElement = container.selectFirst(".title.h3");
                    Element dateElement = container.selectFirst(".text-primary.mb-3");

                    if (titleElement != null && dateElement != null) {
                        String title = titleElement.text();
                        String dateText = dateElement.text();
                        String link = titleElement.attr("href");

                        if (!link.startsWith("http")) {
                            link = "https://weii.pollub.pl" + link;
                        }

                        if (title.toLowerCase().contains("godziny wolne")) {
                            try {
                                Date date = new SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("pl")).parse(dateText);
                                freeDayEvents.add(new FreeDayEvent(date, title, link));
                            } catch (ParseException e) {
                                System.err.println("Nie można sparsować daty: " + dateText);
                            }
                        }
                    }
                }
            }

            // Sortowanie wydarzeń od najstarszego do najnowszego
            freeDayEvents.sort(Comparator.comparing(FreeDayEvent::date));

            System.out.println("\n\nGodziny wolne od zajęć:");
            for (FreeDayEvent event : freeDayEvents) {
                System.out.println("Data: " + new SimpleDateFormat("dd.MM.yyyy").format(event.date()));
                System.out.println("Tytuł: " + event.title());
                System.out.println("Link: " + event.link());
                System.out.println("=================================");
            }

        } catch (IOException e) {
            System.err.println("Błąd podczas pobierania danych: " + e.getMessage());
        }
    }

    public record FreeDayEvent(Date date, String title, String link) {
    }

    // Metoda do filtrowania pracowników z tytułem "dr" lub "dr inż."
    private static boolean FilterStuff(String name) {
        return name.startsWith("dr inż.") || (name.startsWith("dr") && !name.contains("hab") && !name.contains("mgr"));
    }
}
