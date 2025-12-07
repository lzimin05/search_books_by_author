package com.bookclient.service;

import com.bookclient.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class BookClientService {
    
    private static final Logger log = LoggerFactory.getLogger(BookClientService.class);
    
    private final WebClient webClient;
    
    @Value("${webclient.max-retries}")
    private int maxRetries;
    
    @Value("${webclient.retry-delay}")
    private int retryDelay;

    public BookClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Поиск книг по автору через WebClient
     * Применяется:
     * - обработка Flux
     * - retry с backoff
     * - обработка ошибок
     * - таймауты (настроены в конфигурации)
     */
    public Flux<Book> searchBooksByAuthor(String author) {
        log.info("Отправка запроса на Service B для поиска книг автора: {}", author);
        
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/books/search")
                        .queryParam("author", author)
                        .build())
                .retrieve()
                .bodyToFlux(Book.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(retryDelay))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest))
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Повторная попытка #{} из-за: {}", 
                                    retrySignal.totalRetries() + 1, 
                                    retrySignal.failure().getMessage())))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Ошибка HTTP {}: {}", ex.getStatusCode(), ex.getMessage());
                    return Flux.empty();
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Ошибка при выполнении запроса: {}", ex.getMessage());
                    return Flux.empty();
                })
                .doOnComplete(() -> log.info("Получены все книги от Service B"))
                .doOnError(error -> log.error("Ошибка в потоке данных: {}", error.getMessage()));
    }

    /**
     * Подсчет количества книг автора
     */
    public Mono<Long> countBooksByAuthor(String author) {
        return searchBooksByAuthor(author)
                .count()
                .doOnSuccess(count -> log.info("Найдено {} книг автора '{}'", count, author));
    }

    /**
     * Фильтрация книг по году
     */
    public Flux<Book> searchBooksByAuthorAndYear(String author, int minYear) {
        return searchBooksByAuthor(author)
                .filter(book -> book.getYear() >= minYear)
                .doOnNext(book -> log.debug("Книга соответствует фильтру: {}", book.getTitle()));
    }
}
