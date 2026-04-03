package geoinfo.server.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.net.URLEncoder;

public class NewsService {

    public static String getNewsInfo(String input) {
        try{
            String keyword = input.trim();
            String encoded = URLEncoder.encode(keyword, "UTF-8");
            String rssUrl = "https://news.google.com/rss/search?q=" + encoded + "&hl=en-US&gl=US&ceid=US:en";
            URL url = new URL(rssUrl);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(url.openStream());

            NodeList items = doc.getElementsByTagName("item");

            int limit = Math.min(5, items.getLength());

            if (limit == 0) {
                return "Không có tin tức nào được tìm thấy cho: " + keyword;
            }

            StringBuilder output = new StringBuilder();
            for(int i = 0; i < limit; i++){
                Element item = (Element) items.item(i);
                String title = item.getElementsByTagName("title").item(0) != null
                        ? item.getElementsByTagName("title").item(0).getTextContent()
                        : "N/A";
                String link = item.getElementsByTagName("link").item(0) != null
                        ? item.getElementsByTagName("link").item(0).getTextContent()
                        : "N/A";
                String pubDate = item.getElementsByTagName("pubDate").item(0) != null
                        ? item.getElementsByTagName("pubDate").item(0).getTextContent()
                        : "N/A";

                output.append((i+1)).append(". ").append(title).append("\n")
                      .append("   Link: ").append(link).append("\n")
                      .append("   Ngày: ").append(pubDate).append("\n\n");
            }

            return output.toString();

        } catch (Exception e){
            return "⚠️ Không thể lấy tin tức: " + e.getMessage();
        }

    }
}
