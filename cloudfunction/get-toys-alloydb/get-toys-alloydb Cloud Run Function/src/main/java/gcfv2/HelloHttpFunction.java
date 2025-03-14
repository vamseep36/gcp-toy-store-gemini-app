package gcfv2;


import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.io.BufferedWriter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.cloud.vertexai.generativeai.preview.ResponseHandler;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import com.google.gson.Gson;


public class HelloHttpFunction implements HttpFunction {
 @Override
 public void service(HttpRequest request, HttpResponse response) throws Exception {
    // Get the request body as a JSON Array.
	JsonArray jsonArray = new JsonArray(); 
    JsonObject requestJson = new Gson().fromJson(request.getReader(), JsonObject.class);
    String searchText = requestJson.get("search").getAsString();
    BufferedWriter writer = response.getWriter();
    String result = "";
	HikariDataSource dataSource = AlloyDbJdbcConnector();
    if(searchText != null && searchText != ""){
        if(searchText.equals("GETALLTOYS")){
            jsonArray = getAllToys(dataSource);
        }else{
            jsonArray = searchToys(searchText, dataSource);
        }
        // Set the response content type and write the JSON array
        response.setContentType("application/json");
        writer.write(jsonArray.toString());
    } else{
        writer.write("Not a valid input");
    }
}

public JsonArray getAllToys(HikariDataSource dataSource) throws Exception{
    String query = "select id, name, description, quantity, price, image_url, text_embeddings from toys";
    JsonArray jsonArray = new JsonArray(); 
    try (Connection connection = dataSource.getConnection()) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
        ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
				   JsonObject jsonObject = new JsonObject();
                   jsonObject.addProperty("id", resultSet.getString("id"));
                   jsonObject.addProperty("name", resultSet.getString("name"));
                   jsonObject.addProperty("description", resultSet.getString("description"));
                   jsonObject.addProperty("quantity", resultSet.getString("quantity"));
                   jsonObject.addProperty("price", resultSet.getString("price"));
                   jsonObject.addProperty("image_url", resultSet.getString("image_url"));
                   jsonArray.add(jsonObject);
            }
          }
        }
    return jsonArray;
}

public JsonArray searchToys(String searchText, HikariDataSource dataSource) throws Exception{
	JsonArray jsonArray = new JsonArray();
    System.out.println("\n\n\n\n\n\n\n*********************** SEARCH TEXT ***********************");
    System.out.println(searchText);
    System.out.println("\n\n\n\n\n\n\n*********************** END SEARCH TEXT ***********************");
    String query = 
    " SELECT  id,name, content, quantity, price, image_url, recommended_text, "
+ " REGEXP_REPLACE(gemini_validation, '[^a-zA-Z,: ]', '', 'g') gemini_validation "
+ " FROM ( "
+ " SELECT   id,name, content, quantity, price, image_url, recommended_text, "
+ "    CAST(ARRAY_AGG(LLM_RESPONSE) AS text) AS gemini_validation FROM ( "
+ " SELECT id,name, content, quantity, price, image_url, recommended_text, "
+ "      json_array_elements( google_ml.predict_row( model_id => 'gemini-1.5', "
+ "          request_body => CONCAT('{  "
+ " \"contents\": [ { \"role\": \"user\", \"parts\": [ { \"text\": \"User wants to buy a toy and this is the " 
+ " description of the toy they wish to buy: ', recommended_text, '. Check if the following product items from the " 
+ " inventory are  close enough to really, contextually match the user description. "
+ " Here are the items: ', content,'. "
+ "  Return a ONE-LINE response with 3 values: 1) MATCH: if the 2 contexts are reasonably matching in terms of any "
+ " of the color or color family specified in the list, approximate style match with any of the styles mentioned in "
+ " the user search text: This should be a simple YES or NO. Choose NO only if it is completely irrelevant to users "
+ " search criteria. 2) PERCENTAGE: percentage of match, make sure that this percentage is accurate 3) DIFFERENCE: "
+ " A clear one-line easy description of the difference between the 2 products. Remember if the user search text "
+ " says that some attribute should not be there, and the record has it, it should be a NO match. \" } ] } ] }'  "
+ " )::json))-> 'candidates' -> 0 -> 'content' -> 'parts' -> 0 -> 'text' ::text AS LLM_RESPONSE "
+ "    FROM "  
+ "      (SELECT id, name, description as content, quantity, price, image_url, "
+ " '" + searchText + "' AS recommended_text "
+ "      FROM toys ORDER BY text_embeddings <=> embedding('text-embedding-005',  '" + searchText + "')::vector "
+ "      LIMIT 10) as xyz ) AS X "
+ "  GROUP BY id,name, content, quantity, price, image_url, recommended_text "
+ "     ) AS final_matches "
+ " WHERE REGEXP_REPLACE(gemini_validation, '[^a-zA-Z,: ]', '', 'g') LIKE '%MATCH%:%YES%' ";
    
    try (Connection connection = dataSource.getConnection()) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
        ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
				   JsonObject jsonObject = new JsonObject();
                   jsonObject.addProperty("id", resultSet.getString("id"));
                   jsonObject.addProperty("name", resultSet.getString("name"));
                   jsonObject.addProperty("description", resultSet.getString("content"));
                   jsonObject.addProperty("quantity", resultSet.getString("quantity"));
                   jsonObject.addProperty("price", resultSet.getString("price"));
                   jsonObject.addProperty("image_url", resultSet.getString("image_url"));
                   jsonArray.add(jsonObject);
            }
          }
        }
  System.out.println("\n\n\n\n\n\n\n*********************** jsonArray SIZE***********************");
  System.out.println(jsonArray.size());
  System.out.println("\n\n\n\n\n\n\n*********************** END jsonArray SIZE***********************");
    return jsonArray;
}



 public  HikariDataSource AlloyDbJdbcConnector() {
  HikariDataSource dataSource;
  String ALLOYDB_DB = "postgres";
  String ALLOYDB_USER = "postgres";
  String ALLOYDB_PASS = "alloydb";
  String ALLOYDB_INSTANCE_NAME = "projects/<<YOUR_PROJECT>>/locations/us-central1/clusters/vector-cluster/instances/vector-instance";
   HikariConfig config = new HikariConfig();
   config.setJdbcUrl(String.format("jdbc:postgresql:///%s", ALLOYDB_DB));
   config.setUsername(ALLOYDB_USER); // e.g., "postgres"
   config.setPassword(ALLOYDB_PASS); // e.g., "secret-password"
   config.addDataSourceProperty("socketFactory", "com.google.cloud.alloydb.SocketFactory");
   config.addDataSourceProperty("alloydbInstanceName", ALLOYDB_INSTANCE_NAME);
   dataSource = new HikariDataSource(config);
   return dataSource;
}
}