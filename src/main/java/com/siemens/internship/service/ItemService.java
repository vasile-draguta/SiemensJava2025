package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    // Using synchronized list to ensure thread safety
    private final List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());
    // Using AtomicInteger to ensure thread safety
    private final AtomicInteger processedCount = new AtomicInteger(0);

    // Shutdown hook to clean up executor resources, this is triggered when the
    // application is closed
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        validateId(id);
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        validateId(id);
        itemRepository.deleteById(id);
    }

    // The original implementation had several critical issues:
    // 1. It returned the list immediately without waiting for async tasks to
    // complete
    // 2. It used non-thread-safe collections for shared state (regular ArrayList)
    // 3. It had race conditions in updating processedCount
    // 4. It used runAsync instead of supplyAsync, making it difficult to collect
    // results
    // 5. The @Async annotation was used incorrectly (method should return
    // CompletableFuture)

    // Changed return type to CompletableFuture<List<Item>> because the annotation
    // is used on a method that returns a List<Item> and the method is asynchronous
    // Now all threads are processed in parallel and the results are collected in a
    // list and returned
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // Create a list to store all the individual futures
        List<CompletableFuture<Item>> futures = new ArrayList<>();

        // Create a future for each item and add it to the list
        for (Long id : itemIds) {
            // Using supplyAsync instead of runAsync to return the processed item
            // This allows collection of results
            CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);

                    Item item = itemRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Item not found: " + id));

                    // Thread-safe increment using AtomicInteger
                    processedCount.incrementAndGet();

                    // Each item is modified by a single thread, so no need to synchronize
                    item.setStatus("PROCESSED");

                    // Spring Data JPA is thread-safe, so no need to synchronize
                    Item savedItem = itemRepository.save(item);

                    // Thread-safe add to synchronized list
                    processedItems.add(savedItem);

                    return savedItem;
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt(); // Stop the thread if got to this catch block
                    throw new CompletionException("Processing interrupted", interruptedException);
                } catch (RuntimeException runtimeException) { // Handle runtime exceptions when the item is not found
                    throw new CompletionException("Error processing item: " + id, runtimeException);
                }
            }, executor);

            futures.add(future); // Add the future to the list to be combined later
        }

        // Combine all futures and wait for all to complete
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join) // Safe to call join here as exceptions are already handled
                        .collect(Collectors.toList()))
                .exceptionally(exception -> {
                    // Log and rethrow as needed
                    System.err.println("Error processing items: " + exception.getMessage());
                    throw new CompletionException(exception);
                });
    }

    private void validateId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (id <= 0) {
            throw new IllegalArgumentException("Id cannot be negative or zero");
        }
    }
}
