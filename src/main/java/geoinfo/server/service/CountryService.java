package geoinfo.server.service;

import geoinfo.server.utils.ApiConnector;
import geoinfo.server.utils.ValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CountryService {
    private static final String ALL_COUNTRIES_URL =
            "https://restcountries.com/v3.1/all?fields=name,capital,altSpellings,cca2,cca3,latlng,population,currencies,languages,borders";
    private static final String FLAG_URL_TEMPLATE = "https://flagcdn.com/w320/%s.png";
    private static final String COUNTRY_ALIAS_RESOURCE = "/data/vietsub.csv";

    private static JSONArray countriesCache;
    private static volatile Map<String, String> countryAliasCache;

    public static String getCountryInfo(String input) {
        try {
            JSONObject country = getValidatedCountry(input);
            JSONObject nameObj = country.optJSONObject("name");
            String commonName = nameObj == null ? input : nameObj.optString("common", input);
            String cca2 = country.optString("cca2", "");

            JSONArray latlngArr = country.optJSONArray("latlng");
            String coordinates = latlngArr == null ? "Empty" : latlngArr.toString();
            String population = String.valueOf(country.optLong("population", 0));
            String currencies = buildCurrencies(country.optJSONObject("currencies"));
            String languages = buildLanguages(country.optJSONObject("languages"));
            String neighboringCountries = formatBorders(country.optJSONArray("borders"));
            String currentWeather = CityService.getWeatherSummary(getCapitalOrCountryName(country, commonName));

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("type", "country");
            response.put("name", commonName);
            response.put("coordinates", coordinates);
            response.put("population", population);
            response.put("currencies", currencies);
            response.put("languages", languages);
            response.put("neighboringCountries", neighboringCountries);
            response.put("currentWeather", currentWeather);
            response.put("flagUrl", buildFlagUrl(cca2));
            response.put("moreInfoRequest", "country-more:" + commonName);
            response.put("moreInfoLabel", "More Information");
            return response.toString(2);
        } catch (Exception e) {
            return createErrorResponse("Error getting country data: " + e.getMessage());
        }
    }

    public static String getCountryMoreInfo(String input) {
        try {
            JSONObject country = getValidatedCountry(input);
            JSONObject nameObj = country.optJSONObject("name");
            String commonName = nameObj == null ? input : nameObj.optString("common", input);
            String cca2 = country.optString("cca2", "");

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("type", "countryMoreInfo");
            response.put("name", commonName);
            response.put("news", extractItems(NewsService.getNewsInfo(commonName)));
            response.put("attractions", extractItems(AttractionService.getAttractionInfo(cca2)));
            return response.toString(2);
        } catch (Exception e) {
            return createErrorResponse("Error getting country data: " + e.getMessage());
        }
    }

    public static String reloadCountries() {
        try {
            countriesCache = null;
            countryAliasCache = null;
            ensureCountriesLoaded();
            return new JSONObject()
                    .put("status", "success")
                    .put("type", "country")
                    .put("message", "Da tai lai du lieu quoc gia.")
                    .toString(2);
        } catch (Exception e) {
            return new JSONObject()
                    .put("status", "error")
                    .put("type", "country")
                    .put("message", "Error reloading country data: " + e.getMessage())
                    .toString(2);
        }
    }

    private static JSONObject getValidatedCountry(String input) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("No data");
        }

        input = input.replaceAll("\\s+", " ").trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("No data.");
        }
        if (input.length() < 2) {
            throw new IllegalArgumentException("My country name is too short.");
        }
        if (input.length() > 100) {
            throw new IllegalArgumentException("My country name is too long.");
        }
        if (!ValidationUtils.isValidLocationName(input)) {
            throw new IllegalArgumentException("My  country name is invalid characters.");
        }

        String originalInput = input;
        input = resolveCountryAlias(input);
        boolean aliasResolved = !originalInput.equals(input);
        boolean allowShortCodeAltMatch = !aliasResolved && isUppercaseCountryCodeQuery(originalInput);
        ensureCountriesLoaded();
        JSONObject country = findCountry(input, allowShortCodeAltMatch);
        if (country == null) {
            throw new IllegalArgumentException("Not found country.");
        }
        return country;
    }

    private static synchronized JSONObject findCountry(String keyword, boolean allowShortCodeAltMatch) {
        String normalizedKeyword = normalize(keyword);
        String compactKeyword = normalizeCompact(keyword);
        String accentInsensitiveKeyword = normalizeAccentInsensitive(keyword);
        String compactAccentInsensitiveKeyword = normalizeCompactAccentInsensitive(keyword);

        JSONObject exactCommonMatch = null;
        JSONObject exactOfficialMatch = null;
        JSONObject exactAltSpellingMatch = null;
        JSONObject compactCommonMatch = null;
        JSONObject compactOfficialMatch = null;
        JSONObject compactAltSpellingMatch = null;
        JSONObject accentInsensitiveCommonMatch = null;
        JSONObject accentInsensitiveOfficialMatch = null;
        JSONObject accentInsensitiveAltSpellingMatch = null;
        JSONObject compactAccentInsensitiveCommonMatch = null;
        JSONObject compactAccentInsensitiveOfficialMatch = null;
        JSONObject compactAccentInsensitiveAltSpellingMatch = null;
        JSONObject partialMatch = null;

        for (int i = 0; i < countriesCache.length(); i++) {
            JSONObject country = countriesCache.getJSONObject(i);
            if (country == null) {
                continue;
            }

            JSONObject name = country.optJSONObject("name");
            String commonName = name == null ? "" : normalize(name.optString("common"));
            String officialName = name == null ? "" : normalize(name.optString("official"));
            String compactCommonName = normalizeCompact(commonName);
            String compactOfficialName = normalizeCompact(officialName);
            String accentInsensitiveCommonName = normalizeAccentInsensitive(commonName);
            String accentInsensitiveOfficialName = normalizeAccentInsensitive(officialName);
            String compactAccentInsensitiveCommonName = normalizeCompactAccentInsensitive(commonName);
            String compactAccentInsensitiveOfficialName = normalizeCompactAccentInsensitive(officialName);

            if (normalizedKeyword.equals(commonName)) {
                exactCommonMatch = country;
                break;
            }
            if (exactOfficialMatch == null && normalizedKeyword.equals(officialName)) {
                exactOfficialMatch = country;
            }
            if (compactCommonMatch == null && compactKeyword.equals(compactCommonName)) {
                compactCommonMatch = country;
            }
            if (compactOfficialMatch == null && compactKeyword.equals(compactOfficialName)) {
                compactOfficialMatch = country;
            }
            if (accentInsensitiveCommonMatch == null && accentInsensitiveKeyword.equals(accentInsensitiveCommonName)) {
                accentInsensitiveCommonMatch = country;
            }
            if (accentInsensitiveOfficialMatch == null && accentInsensitiveKeyword.equals(accentInsensitiveOfficialName)) {
                accentInsensitiveOfficialMatch = country;
            }
            if (compactAccentInsensitiveCommonMatch == null
                    && compactAccentInsensitiveKeyword.equals(compactAccentInsensitiveCommonName)) {
                compactAccentInsensitiveCommonMatch = country;
            }
            if (compactAccentInsensitiveOfficialMatch == null
                    && compactAccentInsensitiveKeyword.equals(compactAccentInsensitiveOfficialName)) {
                compactAccentInsensitiveOfficialMatch = country;
            }

            JSONArray altSpellings = country.optJSONArray("altSpellings");
            if (altSpellings != null) {
                for (int j = 0; j < altSpellings.length(); j++) {
                    String altSpelling = altSpellings.optString(j);
                    if (!allowShortCodeAltMatch && isShortCountryCodeToken(altSpelling)) {
                        continue;
                    }
                    String normalizedAlt = normalize(altSpelling);
                    String compactAlt = normalizeCompact(altSpelling);
                    String accentInsensitiveAlt = normalizeAccentInsensitive(altSpelling);
                    String compactAccentInsensitiveAlt = normalizeCompactAccentInsensitive(altSpelling);

                    if (exactAltSpellingMatch == null && normalizedKeyword.equals(normalizedAlt)) {
                        exactAltSpellingMatch = country;
                    }
                    if (compactAltSpellingMatch == null && compactKeyword.equals(compactAlt)) {
                        compactAltSpellingMatch = country;
                    }
                    if (accentInsensitiveAltSpellingMatch == null
                            && accentInsensitiveKeyword.equals(accentInsensitiveAlt)) {
                        accentInsensitiveAltSpellingMatch = country;
                    }
                    if (compactAccentInsensitiveAltSpellingMatch == null
                            && compactAccentInsensitiveKeyword.equals(compactAccentInsensitiveAlt)) {
                        compactAccentInsensitiveAltSpellingMatch = country;
                    }
                }
            }

            if (partialMatch == null && normalizedKeyword.length() >= 4
                    && (commonName.startsWith(normalizedKeyword) || officialName.startsWith(normalizedKeyword))) {
                partialMatch = country;
            }
        }

        if (exactCommonMatch != null) return exactCommonMatch;
        if (exactOfficialMatch != null) return exactOfficialMatch;
        if (exactAltSpellingMatch != null) return exactAltSpellingMatch;
        if (compactCommonMatch != null) return compactCommonMatch;
        if (compactOfficialMatch != null) return compactOfficialMatch;
        if (compactAltSpellingMatch != null) return compactAltSpellingMatch;
        if (accentInsensitiveCommonMatch != null) return accentInsensitiveCommonMatch;
        if (accentInsensitiveOfficialMatch != null) return accentInsensitiveOfficialMatch;
        if (accentInsensitiveAltSpellingMatch != null) return accentInsensitiveAltSpellingMatch;
        if (compactAccentInsensitiveCommonMatch != null) return compactAccentInsensitiveCommonMatch;
        if (compactAccentInsensitiveOfficialMatch != null) return compactAccentInsensitiveOfficialMatch;
        if (compactAccentInsensitiveAltSpellingMatch != null) return compactAccentInsensitiveAltSpellingMatch;
        return partialMatch;
    }

    private static synchronized void ensureCountriesLoaded() throws Exception {
        if (countriesCache != null) {
            return;
        }

        Document doc = ApiConnector.get(ALL_COUNTRIES_URL);
        if (doc == null) {
            throw new IllegalStateException("Unable to connect country API: " + ALL_COUNTRIES_URL);
        }
        String body = doc.text();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Data is empty or invalid.");
        }
        countriesCache = new JSONArray(body);
    }

    private static String buildCurrencies(JSONObject currencies) {
        if (currencies == null || currencies.isEmpty()) {
            return "Empty";
        }

        StringBuilder result = new StringBuilder();
        for (String code : currencies.keySet()) {
            JSONObject currency = currencies.optJSONObject(code);
            if (currency == null) {
                continue;
            }

            String name = currency.optString("name", "");
            String symbol = currency.optString("symbol", "");
            if (!name.isBlank()) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(name);
                if (!symbol.isBlank()) {
                    result.append(" - ").append(symbol);
                }
            }
        }

        return result.isEmpty() ? "Empty" : result.toString();
    }

    private static String buildLanguages(JSONObject languages) {
        if (languages == null || languages.isEmpty()) {
            return "Empty";
        }

        StringBuilder result = new StringBuilder();
        for (String code : languages.keySet()) {
            String language = languages.optString(code, "");
            if (!language.isBlank()) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(language);
            }
        }

        return result.isEmpty() ? "Empty" : result.toString();
    }

    private static String getCapitalOrCountryName(JSONObject country, String fallbackName) {
        JSONArray capitalArray = country.optJSONArray("capital");
        if (capitalArray != null && !capitalArray.isEmpty()) {
            String capital = capitalArray.optString(0, "");
            if (!capital.isBlank()) {
                return capital;
            }
        }
        return fallbackName;
    }

    private static String formatBorders(JSONArray borders) {
        if (borders == null || borders.isEmpty()) {
            return "Empty";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < borders.length(); i++) {
            String borderCode = borders.optString(i, "");
            if (borderCode.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(findCountryNameByCca3(borderCode));
        }

        return result.isEmpty() ? "Empty" : result.toString();
    }

    private static String findCountryNameByCca3(String cca3) {
        for (int i = 0; i < countriesCache.length(); i++) {
            JSONObject country = countriesCache.optJSONObject(i);
            if (country == null) {
                continue;
            }

            if (cca3.equalsIgnoreCase(country.optString("cca3", ""))) {
                JSONObject name = country.optJSONObject("name");
                if (name != null) {
                    String commonName = name.optString("common", "");
                    if (!commonName.isBlank()) {
                        return commonName;
                    }
                }
                return cca3;
            }
        }

        return cca3;
    }

    private static String buildFlagUrl(String cca2) {
        if (cca2 == null || cca2.isBlank()) {
            return "";
        }
        return FLAG_URL_TEMPLATE.formatted(cca2.trim().toLowerCase());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase()
                .replaceAll("[\\p{Punct}’‘`]", " ")
                .replaceAll("\\s+", " ");
    }

    private static String normalizeCompact(String value) {
        return normalize(value).replace(" ", "");
    }

    private static String normalizeAccentInsensitive(String value) {
        String normalized = normalize(value);
        String decomposed = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    private static String normalizeCompactAccentInsensitive(String value) {
        return normalizeAccentInsensitive(value).replace(" ", "");
    }

    private static boolean isUppercaseCountryCodeQuery(String input) {
        if (input == null) {
            return false;
        }
        String compact = input.replaceAll("\\s+", "");
        return compact.matches("[A-Z]{2,3}");
    }

    private static boolean isShortCountryCodeToken(String value) {
        if (value == null) {
            return false;
        }
        return value.trim().matches("[A-Z]{2,3}");
    }

    private static String resolveCountryAlias(String keyword) {
        ensureCountryAliasesLoaded();
        if (countryAliasCache == null || countryAliasCache.isEmpty()) {
            return keyword;
        }

        String normalizedKeyword = normalizeAccentInsensitive(keyword);
        String compactKeyword = normalizeCompactAccentInsensitive(keyword);

        String mapped = countryAliasCache.get(normalizedKeyword);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }

        mapped = countryAliasCache.get(compactKeyword);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return keyword;
    }

    private static synchronized void ensureCountryAliasesLoaded() {
        if (countryAliasCache != null) {
            return;
        }

        Map<String, String> aliases = new HashMap<>();
        try (InputStream stream = CountryService.class.getResourceAsStream(COUNTRY_ALIAS_RESOURCE)) {
            if (stream == null) {
                System.err.println("Country alias file not found: " + COUNTRY_ALIAS_RESOURCE);
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        addCountryAlias(aliases, line);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Unable to load country aliases: " + e.getMessage());
        }

        addDefaultCountryAliases(aliases);
        countryAliasCache = aliases.isEmpty() ? Map.of() : Map.copyOf(aliases);
    }

    private static void addCountryAlias(Map<String, String> aliases, String rawLine) {
        if (rawLine == null) {
            return;
        }

        String line = rawLine.replace("\uFEFF", "").trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        if (line.length() >= 2 && line.startsWith("\"") && line.endsWith("\"")) {
            line = line.substring(1, line.length() - 1).trim();
        }

        int splitIndex = line.indexOf(',');
        if (splitIndex <= 0 || splitIndex >= line.length() - 1) {
            return;
        }

        String alias = stripWrappingQuotes(line.substring(0, splitIndex));
        String canonicalName = stripWrappingQuotes(line.substring(splitIndex + 1));
        addAliasMapping(aliases, alias, canonicalName);
    }

    private static void addDefaultCountryAliases(Map<String, String> aliases) {
        addAliasMapping(aliases, "my", "United States");
        addAliasMapping(aliases, "hoa ky", "United States");
    }

    private static void addAliasMapping(Map<String, String> aliases, String alias, String canonicalName) {
        if (alias == null || canonicalName == null) {
            return;
        }

        String normalizedCanonicalName = canonicalName.trim();
        if (alias.isBlank() || normalizedCanonicalName.isBlank()) {
            return;
        }

        String normalizedAlias = normalizeAccentInsensitive(alias);
        String compactAlias = normalizeCompactAccentInsensitive(alias);
        aliases.putIfAbsent(normalizedAlias, normalizedCanonicalName);
        aliases.putIfAbsent(compactAlias, normalizedCanonicalName);
    }

    private static String stripWrappingQuotes(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            return normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static String createErrorResponse(String message) {
        return new JSONObject()
                .put("status", "error")
                .put("type", "country")
                .put("message", message)
                .toString(2);
    }

    private static JSONArray extractItems(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);
            return json.optJSONArray("items") == null ? new JSONArray() : json.optJSONArray("items");
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = "";

        while (!input.equalsIgnoreCase("exit")) {
            System.out.print("Country name: ");
            input = scanner.nextLine();

            if (!input.equalsIgnoreCase("exit")) {
                System.out.println(getCountryInfo(input));
            }
        }

        scanner.close();
    }
}
