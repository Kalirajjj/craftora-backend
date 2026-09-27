package com.craftora.craftora_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", nullable = false)
    @JsonProperty("image")
    private String imageUrl;

    private String category;

    private String badge;

    private BigDecimal rating;

    private Boolean featured = false;

    private String dimensions;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "in_stock")
    private Boolean inStock = true;

    @ElementCollection
    @CollectionTable(
            name = "product_available_colors",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "color", nullable = false)
    private List<String> availableColors = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public Integer getWeightGrams() { return weightGrams; }
    public void setWeightGrams(Integer weightGrams) { this.weightGrams = weightGrams; }

    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }

    public List<String> getAvailableColors() { return availableColors; }
    public void setAvailableColors(List<String> availableColors) {
        this.availableColors = availableColors == null ? new ArrayList<>() : new ArrayList<>(availableColors);
    }

    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) {
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
        this.images.forEach(image -> image.setProduct(this));
    }
    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }
}
