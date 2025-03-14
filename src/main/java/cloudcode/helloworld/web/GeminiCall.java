
package cloudcode.helloworld.web;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import java.util.Base64;
import java.util.Optional;

public class GeminiCall {
  public String imageToBase64String(byte[] imageBytes) {
    String base64Img = Base64.getEncoder().encodeToString(imageBytes);
    return base64Img;
  }

  public String callGemini(String base64ImgWithPrefix) throws Exception {
    String searchText = "";

    // 1. Remove the prefix
    String base64Img = base64ImgWithPrefix.replace("data:image/jpeg;base64,", "");

    // 2. Decode base64 to bytes
    byte[] imageBytes = Base64.getDecoder().decode(base64Img);

    String image = imageToBase64String(imageBytes);

     // Get API key from environment variable
        String apiKey = Optional.ofNullable(System.getenv("GOOGLE_API_KEY"))
                .orElseThrow(() -> new IllegalArgumentException("GOOGLE_API_KEY environment variable not set"));


    ChatLanguageModel gemini = GoogleAiGeminiChatModel.builder()
        .apiKey(apiKey)
        .modelName("gemini-2.0-flash-001")
        .build();

    Response<AiMessage> response = gemini.generate(
        UserMessage.from(
            ImageContent.from(image, "image/jpeg"),
            TextContent.from(
                "The picture has a toy in it. Describe the toy in the image in one line. Do not add any prefix or title to your description. Just describe that toy that you see in the image in one line, do not describe the surroundings and other objects around the toy in the image. If you do not see any toy in the image, send  response stating that no toy is found in the input image.")));
    searchText = response.content().text().trim();
    System.out.println("searchText inside Geminicall: " + searchText);
    return searchText;
  }
}
