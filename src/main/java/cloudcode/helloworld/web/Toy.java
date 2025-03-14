package cloudcode.helloworld.web;

import java.util.List;

public class Toy {
    private String id;
    private String name;
    private String description;
    private String imageURL;
    private int quantity;
    private float price;
    private byte[] imageBytes;
    private float[] textEmb;
    private float[] imageEmb;
    private List<Toy> toysMatching;
    private String imageBase64;

    public List<Toy> getToysMatching() {
        return toysMatching;
    }

    public void setToysMatching(List<Toy> toysMatching) {
        this.toysMatching = toysMatching;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public float[] getTextEmb() {
        return textEmb;
    }

    public void setTextEmb(float[] textEmb) {
        this.textEmb = textEmb;
    }

    public float[] getImageEmb() {
        return imageEmb;
    }

    public void setImageEmb(float[] imageEmb) {
        this.imageEmb = imageEmb;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

}
