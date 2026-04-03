package geoinfo.server.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

public class CountryService {

    // lay thong tin quoc gia tu API restcountries.com
    public static String getCountryInfo(String input) {
        String query = input == null ? "" : input.trim();
        if (query.isEmpty()) {
            return "Vui long nhap ten quoc gia.";
        }

        try {
            JSONObject json = fetchBestCountry(query);
            if (json == null) {
                return "Không tìm thấy quốc gia: " + input;
            }

            // Ten quoc gia
            String tenQuocGia = "N/A";
            if (json.has("name")) {
                JSONObject nameObj = json.getJSONObject("name");
                if (nameObj.has("official")) {
                    tenQuocGia = nameObj.getString("official");
                } else if (nameObj.has("common")) {
                    tenQuocGia = nameObj.getString("common");
                }
            }

            // Thu do
            String thuDo = json.has("capital") ? json.getJSONArray("capital").getString(0) : "N/A";

            // mui gio
            String muiGio = json.has("timezones") ? json.getJSONArray("timezones").getString(0) : "N/A";

            // Khu vuc
            String khuVuc = json.has("region") ? json.getString("region") : "N/A";

            // Toa do
            String toaDo = "N/A";
            double lat = 0;
            double lng = 0;
            if (json.has("latlng")) {
                JSONArray latlngArr = json.getJSONArray("latlng");
                if (latlngArr.length() >= 2) {
                    lat = latlngArr.getDouble(0);
                    lng = latlngArr.getDouble(1);
                    toaDo = lat + ", " + lng;
                }
            }

            // Dien tich
            String dienTich = "N/A";
            if (json.has("area")) {
                dienTich = String.format("%,.0f km2", json.getDouble("area"));
            }

            // Dan so
            String danSo = "N/A";
            if (json.has("population")) {
                danSo = String.format("%,d", json.getLong("population"));
            }

            // Tien te
            StringBuilder tienTe = new StringBuilder();
            if (json.has("currencies")) {
                JSONObject currencies = json.getJSONObject("currencies");
                for (String code : currencies.keySet()) {
                    JSONObject c = currencies.getJSONObject(code);
                    String name = c.has("name") ? c.getString("name") : code;
                    String symbol = c.has("symbol") ? c.getString("symbol") : "";
                    if (symbol.isEmpty()) {
                        tienTe.append(name).append(", ");
                    } else {
                        tienTe.append(name).append(" (").append(symbol).append("), ");
                    }
                }
                if (tienTe.length() > 0) {
                    tienTe.setLength(tienTe.length() - 2);
                }
            }
            if (tienTe.length() == 0) {
                tienTe.append("N/A");
            }

            // Ngon ngu
            StringBuilder ngonNgu = new StringBuilder();
            if (json.has("languages")) {
                JSONObject languages = json.getJSONObject("languages");
                for (String code : languages.keySet()) {
                    ngonNgu.append(languages.getString(code)).append(", ");
                }
                if (ngonNgu.length() > 0) {
                    ngonNgu.setLength(ngonNgu.length() - 2);
                }
            }
            if (ngonNgu.length() == 0) {
                ngonNgu.append("N/A");
            }

            // Quoc ky
            String quocKy = "";
            if (json.has("flag")) {
                quocKy = json.getString("flag");
            } else if (json.has("flags")) {
                JSONObject flagsObj = json.getJSONObject("flags");
                quocKy = flagsObj.optString("png", "");
            }
            if (quocKy.isBlank()) {
                quocKy = "(không có dữ liệu quốc kỳ)";
            }

            // Quoc gia lang gieng
            String langGieng = "Không có quốc gia láng giềng";
            if (json.has("borders")) {
                JSONArray borders = json.getJSONArray("borders");
                if (borders.length() > 0) {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < borders.length(); i++) {
                        b.append(borders.getString(i)).append(", ");
                    }
                    b.setLength(b.length() - 2);
                    langGieng = b.toString();
                }
            }

            String cacDiaDiem = getTopPlaces(lat, lng);

            // Ket qua tra ve
            return "===============================================\n"
                    + "Quốc gia: " + tenQuocGia + "\n"
                    + "Thủ đô: " + thuDo + "\n"
                    + "Múi giờ: " + muiGio + "\n"
                    + "Khu vực: " + khuVuc + "\n"
                    + "Toạ độ: " + toaDo + "\n"
                    + "Diện tích : " + dienTich + "\n"
                    + "Dân số: " + danSo + "\n"
                    + "Đơn vị tiền tệ: " + tienTe + "\n"
                    + "Ngôn ngữ: " + ngonNgu + "\n"
                    + "Quốc kỳ: " + quocKy + "\n"
                    + "Quốc gia láng giềng: " + langGieng + "\n"
                    + "===============================================\n"
                    + "GỢI Ý ĐỊA ĐIỂM DU LỊCH VÀ KHÁCH SẠN:\n"
                    + cacDiaDiem + "\n"
                    + "===============================================";

        } catch (Exception e) {
            return "Loi khi lay du lieu quoc gia: " + e.getMessage();
        }
    }

    private static JSONObject fetchBestCountry(String input) throws Exception {
        String encoded = URLEncoder.encode(input, StandardCharsets.UTF_8);

        // Ưu tiên endpoint name để giảm sai lệch kiểu China -> Taiwan
        JSONArray byName = fetchCountryArray("https://restcountries.com/v3.1/name/" + encoded);
        JSONObject best = selectBestCountry(byName, input);
        if (best != null) {
            return best;
        }

        // fallback translation nếu không tìm được bằng name
        JSONArray byTranslation = fetchCountryArray("https://restcountries.com/v3.1/translation/" + encoded);
        return selectBestCountry(byTranslation, input);
    }

    private static JSONArray fetchCountryArray(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .timeout(10000)
                    .execute()
                    .parse();
            return new JSONArray(doc.text());
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static JSONObject selectBestCountry(JSONArray arr, String input) {
        if (arr == null || arr.length() == 0) {
            return null;
        }

        String normalizedInput = normalize(input);
        String upperInput = input.trim().toUpperCase(Locale.ROOT);

        for (int i = 0; i < arr.length(); i++) {
            JSONObject country = arr.getJSONObject(i);
            JSONObject name = country.optJSONObject("name");
            if (name != null) {
                if (normalizedInput.equals(normalize(name.optString("common", "")))) {
                    return country;
                }
                if (normalizedInput.equals(normalize(name.optString("official", "")))) {
                    return country;
                }
            }

            JSONArray altSpellings = country.optJSONArray("altSpellings");
            if (altSpellings != null) {
                for (int j = 0; j < altSpellings.length(); j++) {
                    if (normalizedInput.equals(normalize(altSpellings.optString(j, "")))) {
                        return country;
                    }
                }
            }

            if (upperInput.equals(country.optString("cca2", "")) || upperInput.equals(country.optString("cca3", ""))) {
                return country;
            }
        }

        return arr.getJSONObject(0);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String noAccent = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.trim().toLowerCase(Locale.ROOT);
    }

    // lay thong tin dia diem noi tieng va khach san tu TomTom
    public static String getTopPlaces(double lat, double lng) {
        String apiKey = System.getenv("TOMTOM_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "NCckSIh3VaVeQEqgqXVkg5OX9nYjBa84";
        }

        String attractionsUrl = "https://api.tomtom.com/search/2/nearbySearch/.json?key=" + apiKey
                + "&lat=" + lat + "&lon=" + lng + "&limit=5&radius=30000&categorySet=7376";

        String hotelsUrl = "https://api.tomtom.com/search/2/nearbySearch/.json?key=" + apiKey
                + "&lat=" + lat + "&lon=" + lng + "&limit=5&radius=30000&categorySet=7314";

        try {
            String attractions = fetchFromTomTom(attractionsUrl);
            String hotels = fetchFromTomTom(hotelsUrl);

            StringBuilder sb = new StringBuilder();
            sb.append("📍 ĐỊA ĐIỂM DU LỊCH NỔI TIẾNG:\n");
            sb.append(attractions);
            sb.append("\n");
            sb.append("🏨 KHÁCH SẠN GỢI Ý:\n");
            sb.append(hotels);
            return sb.toString();

        } catch (Exception e) {
            return "   (Lỗi kết nối TomTom: " + e.getMessage() + ")";
        }
    }


    // Ham phu tro de goi TomTom va tra ve String danh sach
    private static String fetchFromTomTom(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .timeout(10000)
                    .execute()
                    .parse();

            JSONObject json = new JSONObject(doc.text());
            if (!json.has("results") || json.getJSONArray("results").length() == 0) {
                return "   (Không tìm thấy dữ liệu trong khu vực này)\n";
            }

            JSONArray results = json.getJSONArray("results");
            StringBuilder list = new StringBuilder();

            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                JSONObject poi = obj.optJSONObject("poi");
                JSONObject address = obj.optJSONObject("address");

                String name = poi != null ? poi.optString("name", "N/A") : "N/A";
                String fullAddr = address != null
                        ? address.optString("freeformAddress", "Địa chỉ đang cập nhật")
                        : "Địa chỉ đang cập nhật";

                list.append("   + ").append(name).append("\n")
                        .append("     (ĐC: ").append(fullAddr).append(")\n");
            }
            return list.toString();

        } catch (Exception e) {
            return "   (Không thể lấy dữ liệu: " + e.getMessage() + ")\n";
        }
    }
}