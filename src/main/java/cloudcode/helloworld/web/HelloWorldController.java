package cloudcode.helloworld.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Optional;

/** Defines a controller to handle HTTP requests */
@Controller
public final class HelloWorldController {

  private static final Logger logger = LoggerFactory.getLogger(HelloWorldController.class);
   // Get API key from environment variable
   public static final String projectID = Optional.ofNullable(System.getenv("PROJECT_ID"))
   .orElseThrow(() -> new IllegalArgumentException("PROJECT_ID environment variable not set"));

  public static final String PROJECT_ID = projectID;
  public static final String LOCATION = "us-central1";
  public static final String MATCHPRICE_ENDPOINT = "https://us-central1-"+PROJECT_ID+".cloudfunctions.net/toolbox-toys";
  public static final String DATABASE_ENDPOINT = "https://us-central1-"+PROJECT_ID+".cloudfunctions.net/get-toys-alloydb";
  
  /**
   * Create an endpoint for the landing page
   *
   */

  @GetMapping("/")
  public ModelAndView home(ModelMap map, Toy toy) throws Exception {
    List<Toy> matchingToys = getAllToys("GETALLTOYS");
    map.addAttribute("toysMatching", matchingToys);
    map.addAttribute("imageBase64", "");
    map.addAttribute("description", "");
    return new ModelAndView("index", map);
  }

  @PostMapping("/search")
  public ModelAndView search(ModelMap map, Toy toy) throws Exception {
    String description = toy.getDescription();
    String imageBase64 = toy.getImageBase64();
    String searchText = "";
    System.out.println("\n\n\n Inside SEARCH \n\n");
    System.out.println(description);
    System.out.println(imageBase64);
    if (description != null && !description.isEmpty()) {
      searchText = description;
    } else {
      if (imageBase64 != null && !imageBase64.isEmpty()) {
        searchText = getGeminiResponse(imageBase64);
      }
    }
    List<Toy> matchingToys = databaseRecommendationEngine(searchText);
    if (matchingToys == null || matchingToys.isEmpty()) {
      map.addAttribute("validationError", "No matching toys found. Let\'s make it for you. Click Create Your Toy!");
    } else {
      map.addAttribute("validationError", "");
    }
    toy.setDescription("");
    toy.setImageBase64("");
    // toy.setToysMatching(matchingToys);
    map.addAttribute("toysMatching", matchingToys);
    map.addAttribute("description", description);
    map.addAttribute("imageBase64", "");
    return new ModelAndView("index", map);
  }

  @PostMapping("/morelikethis")
  public ModelAndView moreLikeThis(ModelMap map, Toy toy) throws Exception {
    String description = toy.getDescription();
    String searchText = "";
    System.out.println(description);
    if (description != null && !description.isEmpty()) {
      searchText = description;
    }
    List<Toy> matchingToys = databaseRecommendationEngine(searchText);
    if (matchingToys == null || matchingToys.isEmpty()) {
      map.addAttribute("validationError", "No matching toys found.");
    } else {
      map.addAttribute("validationError", "");
    }
    toy.setDescription("");
    toy.setImageBase64("");
    // toy.setToysMatching(matchingToys);
    map.addAttribute("toysMatching", matchingToys);
    map.addAttribute("description", description);
    map.addAttribute("imageBase64", "");
    return new ModelAndView("index", map);
  }

  @PostMapping("/toysearch")
  public ModelAndView toyDetails(ModelMap map, Toy toy) throws Exception {
    String toyId = toy.getId();
    System.out.println(toyId);
    if (toyId != null && !toyId.isEmpty()) {
      map.addAttribute("toy", toy);
      return new ModelAndView("toy_details", map);
    } else {
      return new ModelAndView("error"); // Handle case where toy is not found
    }
  }

  @GetMapping("/createatoy")
  public String createToy(ModelMap map) {
    map.addAttribute("toy", new Toy());
    return "create_a_toy";
  }

  @PostMapping("/generate")
  public ModelAndView generate(ModelMap map, Toy toy) throws Exception {
    try {
      GenerateToy imageGen = new GenerateToy();
      String project = PROJECT_ID;
      String location = "us-central1";
      String description = toy.getDescription();
      String base64 = imageGen.generateImage(project, location, description);
      toy.setDescription(description);
      String priceValue = matchPrice(description);
      map.addAttribute("toy", toy);
      map.addAttribute("imagestring", "data:image/jpg;base64," + base64);
      map.addAttribute("pricePredict", priceValue);
      // map.addAttribute("pricedescription", priceDescription);
    } catch (Exception e) {
      System.out.println(e);
      map.addAttribute("description", "");
    }
    return new ModelAndView("create_a_toy", map);
  }

  public String matchPrice(String descriptionText) throws Exception {
    String priceValue = "0.0";
    String endPoint = MATCHPRICE_ENDPOINT;
    try {
      URL url = new URL(endPoint);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setDoOutput(true);

      // Create JSON payload
      Gson gson = new Gson();
      Map<String, String> data = new HashMap<>();
      data.put("name", descriptionText);
      String jsonInputString = gson.toJson(data);

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = jsonInputString.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();

        try {
          priceValue = response.toString();
        } catch (Exception e) { // Handle invalid JSON
          System.err.println("Error parsing response: " + e.getMessage());
        }
      } else {
        System.out.println("POST request of TOOLBOX not working");
      }
    } catch (Exception e) {
      System.out.println(e);
    }

    return priceValue;
  }

  public Toy getToyById(String toyId, List<Toy> toys) throws Exception {
    for (Toy toy : toys) {
      if (toyId.equals(toy.getId())) {
        return toy;
      }
    }
    return null; // If toy not found.
  }

  public List<Toy> getAllToys(String searchText) throws Exception {
    List<Toy> toys = new ArrayList<Toy>();
    toys = databaseRecommendationEngine(searchText);
    return toys;
  }

  /*
   * Method that is invoked to do Vector Search against database data.
   */
  public List<Toy> databaseRecommendationEngine(String searchText) throws Exception {
    List<Toy> toys = new ArrayList<Toy>();
    String endpoint = DATABASE_ENDPOINT;
    // code to invoke the endpoint and pass the string request to retrieve Gemini
    // validated vector search results
    System.out.println("Inside calling DATABASE endpoint function*******************");
    try {
      URL url = new URL(endpoint);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setDoOutput(true);

      // Create JSON payload
      Gson gson = new Gson();
      Map<String, String> data = new HashMap<>();
      data.put("search", searchText);
      String jsonInputString = gson.toJson(data);

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = jsonInputString.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();
        String jsonString = response.toString();
        // Parse JSON array response
        try {
          JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
          for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            String id = jsonObject.get("id").getAsString();
            String name = jsonObject.get("name").getAsString();
            String description = jsonObject.get("description").getAsString().trim();
            int quantity = jsonObject.get("quantity").getAsInt();
            float price = jsonObject.get("price").getAsFloat();
            String image_url = jsonObject.get("image_url").getAsString();
            Toy toy = new Toy();
            toy.setId(id);
            toy.setName(name);
            toy.setDescription(description);
            toy.setQuantity(quantity);
            toy.setPrice(price);
            toy.setImageURL(image_url);

            toys.add(toy);
          }
        } catch (Exception e) { // Handle invalid JSON
          System.err.println("Error parsing JSON: " + e.getMessage());
        }
      } else {
        System.out.println("POST request not worked");
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    return toys;
  }

  public String getGeminiResponse(String imageBase64) throws Exception {
    String responseText = "";
    GeminiCall gm = new GeminiCall();
    responseText = gm.callGemini(imageBase64);
    return responseText;
  }

}
