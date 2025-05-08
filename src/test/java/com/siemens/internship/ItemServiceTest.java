package com.siemens.internship;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;

// Unit tests for ItemService with a mock item repository
@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item validTestItem;
    private List<Item> itemList;

    private static final Long VALID_ID = 1L;
    private static final Long INVALID_ID = -1L;

    @BeforeEach
    void setUp() {
        validTestItem = new Item();
        validTestItem.setId(1L);
        validTestItem.setName("Test Item");
        validTestItem.setDescription("Test Description");
        validTestItem.setStatus("UNPROCESSED");
        validTestItem.setEmail("test@example.com");

        itemList = Arrays.asList(validTestItem);
    }

    @Test
    public void testFindAllItems_ReturnsAllItems() {
        when(itemRepository.findAll()).thenReturn(itemList);

        List<Item> result = itemService.findAll();
        assertEquals(itemList, result);
    }

    @Test
    public void testFindItemById_ValidId_ReturnsItem() {
        when(itemRepository.findById(VALID_ID)).thenReturn(Optional.of(validTestItem));

        Optional<Item> result = itemService.findById(VALID_ID);

        assertEquals(validTestItem, result.get());
    }

    @Test
    public void testFindItemById_NegativeId_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.findById(INVALID_ID);
        });

        assertEquals("Id cannot be negative or zero", exception.getMessage());
    }

    @Test
    public void testFindItemById_NullId_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.findById(null);
        });

        assertEquals("Id cannot be null", exception.getMessage());
    }

    @Test
    public void testDeleteById_ValidId_DeletesItem() {
        doNothing().when(itemRepository).deleteById(VALID_ID);

        itemService.deleteById(VALID_ID);

        verify(itemRepository, times(1)).deleteById(VALID_ID);
    }

    @Test
    public void testDeleteById_NegativeId_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.deleteById(INVALID_ID);
        });

        assertEquals("Id cannot be negative or zero", exception.getMessage());
        verify(itemRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteById_NullId_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.deleteById(null);
        });

        assertEquals("Id cannot be null", exception.getMessage());
        verify(itemRepository, never()).deleteById(any());
    }

    @Test
    public void testSaveItem_ValidItem_ReturnsSavedItem() {
        when(itemRepository.save(validTestItem)).thenReturn(validTestItem);

        Item result = itemService.save(validTestItem);

        assertEquals(validTestItem, result);
        verify(itemRepository, times(1)).save(validTestItem);
    }

    @Test
    public void testSaveItem_NullItem_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.save(null);
        });

        assertEquals("Item cannot be null", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }
}
