package com.bookservice.controller;

import com.bookservice.model.Book;
import com.bookservice.service.BookService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/books")
public class BookController {
    
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/search")
    public Flux<Book> searchBooksByAuthor(@RequestParam String author) {
        return bookService.findBooksByAuthor(author);
    }
}
