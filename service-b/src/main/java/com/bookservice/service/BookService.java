package com.bookservice.service;

import com.bookservice.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class BookService {
    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    
    private static final String[] AUTHORS = {
        "Толстой Л.Н.", "Достоевский Ф.М.", "Пушкин А.С.", "Чехов А.П.", "Булгаков М.А.",
        "Тургенев И.С.", "Гоголь Н.В.", "Лермонтов М.Ю.", "Горький М.", "Шолохов М.А."
    };
    
    private static final String[] GENRES = {
        "Роман", "Повесть", "Рассказ", "Драма", "Поэма"
    };

    /**
     * НЕОПТИМАЛЬНАЯ ЛОГИКА:
     * 1. При каждом запросе генерируется коллекция из 200,000 книг
     * 2. Используется сложный regex с backtracking для поиска
     * 3. Многократная сериализация/десериализация через строки
     * 4. Последовательный перебор всех элементов
     */
    public Flux<Book> findBooksByAuthor(String authorQuery) {
        log.info("Начинается поиск книг по автору: {}", authorQuery);
        long startTime = System.currentTimeMillis();
        
        // Шаг 1: Создаём огромную коллекцию каждый раз заново (неоптимально)
        List<Book> allBooks = generateLargeBookCollection();
        log.info("Создана коллекция из {} книг за {} мс", 
                allBooks.size(), System.currentTimeMillis() - startTime);
        
        // Шаг 2: Формируем сложный regex-паттерн с backtracking
        // Используем составные регулярки с множественными группами и альтернациями
        String complexRegex = ".*(" + 
            authorQuery.chars()
                .mapToObj(c -> "[" + Character.toLowerCase((char)c) + 
                              Character.toUpperCase((char)c) + "]")
                .reduce("", (a, b) -> a + b) + 
            ").*";
        
        Pattern pattern = Pattern.compile(complexRegex);
        
        // Шаг 3: Последовательный перебор с неэффективной фильтрацией
        List<Book> result = new ArrayList<>();
        for (Book book : allBooks) {
            // Многократная десериализация: преобразуем объект в CSV и обратно
            String csvLine = bookToCsv(book);
            Book deserializedBook = csvToBook(csvLine);
            
            // Применяем regex-поиск с backtracking
            if (pattern.matcher(deserializedBook.getAuthor()).matches()) {
                result.add(deserializedBook);
            }
        }
        
        long endTime = System.currentTimeMillis();
        log.info("Найдено {} книг за {} мс", result.size(), endTime - startTime);
        
        return Flux.fromIterable(result);
    }

    /**
     * Генерация большой коллекции книг (200,000 элементов)
     */
    private List<Book> generateLargeBookCollection() {
        List<Book> books = new ArrayList<>(200_000);
        Random random = new Random(42); // фиксированный seed для воспроизводимости
        
        for (long i = 1; i <= 200_000; i++) {
            String author = AUTHORS[random.nextInt(AUTHORS.length)];
            String genre = GENRES[random.nextInt(GENRES.length)];
            
            Book book = new Book(
                i,
                "Книга №" + i,
                author,
                genre,
                1800 + random.nextInt(225),
                100.0 + random.nextDouble() * 900.0
            );
            books.add(book);
        }
        
        return books;
    }

    /**
     * Неэффективная сериализация в CSV (каждый раз заново)
     */
    private String bookToCsv(Book book) {
        return String.format("%d,%s,%s,%s,%d,%.2f",
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getGenre(),
            book.getYear(),
            book.getPrice()
        );
    }

    /**
     * Неэффективная десериализация из CSV (каждый раз заново)
     */
    private Book csvToBook(String csv) {
        String[] parts = csv.split(",");
        Book book = new Book();
        book.setId(Long.parseLong(parts[0]));
        book.setTitle(parts[1]);
        book.setAuthor(parts[2]);
        book.setGenre(parts[3]);
        book.setYear(Integer.parseInt(parts[4]));
        book.setPrice(Double.parseDouble(parts[5]));
        return book;
    }
}
