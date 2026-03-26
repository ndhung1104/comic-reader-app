package com.group09.ComicReader.category.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    // Simulating a dynamic category list from backend instead of hardcoding on client.
    // Ideally this would be fetched from a database table `CategoryEntity`.
    @GetMapping
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(Arrays.asList(
            "All", "Action", "Romance", "Fantasy", "Sci-Fi", "Mystery", 
            "Comedy", "Drama", "Slice of Life", "Adventure"
        ));
    }
}
