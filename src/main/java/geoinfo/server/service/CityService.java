package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import java.io.IOException;

public class CityService {

    public static String getCityInfo(String input) {

        //API Key cho WeatherAPI
        String key = "a7a0ef458a3642009a580805262003";

        String url = "https://api.weatherapi.com/v1/current.json?key=" + key + "&q=" + input.trim() + "&aqi=yes";
        try {
            Document doc = ApiConnector.get(url);
            String responseText = doc.text();
            JSONObject json = new JSONObject(responseText);

            // Kiểm tra nếu API trả về lỗi
            if (json.has("error")) {
                JSONObject error = json.getJSONObject("error");
                String errorMsg = error.has("message") ? error.getString("message") : "Không tìm thấy thành phố";
                return "❌ Lỗi: " + errorMsg + "\n"
                        + "💡 Gợi ý: Kiểm tra xem tên thành phố có đúng không (ví dụ: Hanoi, Ho Chi Minh City, London)";
            }

            JSONObject location = json.getJSONObject("location");
            String name = location.getString("name");
            String region = location.has("region") ? location.getString("region") : "N/A";
            String country = location.getString("country");
            double lat = location.getDouble("lat");
            double lon = location.getDouble("lon");
            String localTime = location.getString("localtime");
            String tzId = location.has("tz_id") ? location.getString("tz_id") : "N/A";

            JSONObject current = json.getJSONObject("current");
            double tempC = current.getDouble("temp_c");
            double tempF = current.getDouble("temp_f");
            String condition = current.getJSONObject("condition").getString("text");
            String conditionIcon = current.getJSONObject("condition").has("icon")
                    ? current.getJSONObject("condition").getString("icon")
                    : "";
            int humidity = current.getInt("humidity");
            double windKph = current.getDouble("wind_kph");
            double windMph = current.getDouble("wind_mph");
            int pressure = current.getInt("pressure_mb");
            double feelsLike = current.getDouble("feelslike_c");
            int visibility = current.getInt("vis_km");
            double uv = current.getDouble("uv");

            // Lấy tin tức
            String news = NewsService.getNewsInfo(name);

            return  "===============================================\n"
                    + "📍 THÔNG TIN THÀNH PHỐ\n"
                    + "===============================================\n"
                    + "Thành phố: " + name + "\n"
                    + "Tỉnh/Vùng: " + region + "\n"
                    + "Quốc gia: " + country + "\n"
                    + "Múi giờ: " + tzId + "\n"
                    + "Tọa độ: " + lat + ", " + lon + "\n"
                    + "Thời gian địa phương: " + localTime + "\n"
                    + "\n"
                    + "🌡️ THÔNG TIN THỜI TIẾT\n"
                    + "===============================================\n"
                    + "Điều kiện: " + condition + "\n"
                    + "Nhiệt độ hiện tại: " + tempC + "°C (" + tempF + "°F)\n"
                    + "Cảm thấy như: " + feelsLike + "°C\n"
                    + "Độ ẩm: " + humidity + "%\n"
                    + "Tốc độ gió: " + windKph + " km/h (" + windMph + " mph)\n"
                    + "Áp suất: " + pressure + " mb\n"
                    + "Tầm nhìn: " + visibility + " km\n"
                    + "Chỉ số UV: " + uv + "\n"
                    + "\n"
                    + "📰 TIN TỨC LIÊN QUAN\n"
                    + "===============================================\n"
                    + news
                    + "\n===============================================";
        } catch (IOException e) {
            return "❌ Lỗi kết nối API: " + e.getMessage() + "\n"
                    + "💡 Gợi ý: Kiểm tra kết nối Internet hoặc thử lại sau";
        } catch (Exception e) {
            return "❌ Lỗi xử lý dữ liệu: " + e.getMessage() + "\n"
                    + "💡 Gợi ý: Kiểm tra xem tên thành phố có đúng không";
        }
    }
}
