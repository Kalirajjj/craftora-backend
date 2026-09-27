package com.craftora.craftora_backend.controller;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.craftora.craftora_backend.model.Product;
import com.craftora.craftora_backend.model.ProductImage;
import com.craftora.craftora_backend.repository.ProductRepository;
import com.craftora.craftora_backend.repository.CustomerCartItemRepository;
import com.craftora.craftora_backend.service.ProductImageStorageService;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository productRepository;
    private final CustomerCartItemRepository cartItemRepository;
    private final ProductImageStorageService imageStorageService;

    public ProductController(ProductRepository productRepository, CustomerCartItemRepository cartItemRepository,
            ProductImageStorageService imageStorageService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.<Product>status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> createProduct(
            @RequestPart("product") Product product,
            @RequestParam("images") List<MultipartFile> images) throws IOException {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required.");
        }
        if (product.getCategory() == null || product.getCategory().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product category is required.");
        }
        if (product.getPrice() == null || product.getPrice().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be greater than zero.");
        }
        if (images == null || images.isEmpty() || images.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload between 1 and 10 product images.");
        }
        product.setId(null);
        product.setName(product.getName().trim());
        product.setCategory(product.getCategory().trim());
        product.setImages(new java.util.ArrayList<>());
        for (int index = 0; index < images.size(); index++) {
            String imageUrl = imageStorageService.store(images.get(index));
            product.addImage(new ProductImage(product, imageUrl, index));
            if (index == 0) product.setImageUrl(imageUrl);
        }
        Product saved = productRepository.save(product);
        return ResponseEntity.created(URI.create("/api/products/" + saved.getId())).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") Product changes,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        validateProduct(changes);
        if (images != null && !images.isEmpty() && images.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload up to 10 product images.");
        }
        product.setName(changes.getName().trim());
        product.setCategory(changes.getCategory().trim());
        product.setDescription(changes.getDescription());
        product.setPrice(changes.getPrice());
        product.setDimensions(changes.getDimensions());
        product.setWeightGrams(changes.getWeightGrams());
        product.setAvailableColors(changes.getAvailableColors());
        product.setFeatured(changes.getFeatured());
        product.setInStock(changes.getInStock());
        if (images != null && !images.isEmpty()) {
            for (ProductImage current : product.getImages()) imageStorageService.delete(current.getImageUrl());
            product.getImages().clear();
            for (int index = 0; index < images.size(); index++) {
                String imageUrl = imageStorageService.store(images.get(index));
                product.addImage(new ProductImage(product, imageUrl, index));
                if (index == 0) product.setImageUrl(imageUrl);
            }
        }
        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        cartItemRepository.deleteAllByProductId(id);
        for (ProductImage image : product.getImages()) imageStorageService.delete(image.getImageUrl());
        imageStorageService.delete(product.getImageUrl());
        productRepository.delete(product);
        return ResponseEntity.noContent().build();
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required.");
        if (product.getCategory() == null || product.getCategory().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product category is required.");
        if (product.getPrice() == null || product.getPrice().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be greater than zero.");
    }
}
