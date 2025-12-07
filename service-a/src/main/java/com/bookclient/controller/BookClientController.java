package com.bookclient.controller;

import com.bookclient.model.Book;
import com.bookclient.service.BookClientService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/client")
public class BookClientController {
    
    private final BookClientService bookClientService;

    public BookClientController(BookClientService bookClientService) {
        this.bookClientService = bookClientService;
    }

    @GetMapping("/search")
    public Flux<Book> searchBooks(@RequestParam String author) {
        return bookClientService.searchBooksByAuthor(author);
    }

    @GetMapping("/count")
    public Mono<Long> countBooks(@RequestParam String author) {
        return bookClientService.countBooksByAuthor(author);
    }

    @GetMapping("/search-by-year")
    public Flux<Book> searchBooksByYear(
            @RequestParam String author,
            @RequestParam(defaultValue = "1900") int minYear) {
        return bookClientService.searchBooksByAuthorAndYear(author, minYear);
    }
}
