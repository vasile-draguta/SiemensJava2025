package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import com.siemens.internship.validation.ValidationExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

// Unit test for the ItemController class with a mock ItemService
@ExtendWith(MockitoExtension.class)
public class ItemControllerTest {

        @Mock
        private ItemService itemService;

        @InjectMocks
        private ItemController itemController;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private Item testItem;
        private Item testItem2;
        private Item testItem3;
        private List<Item> itemList;

        private static final Long VALID_ID = 1L;
        private static final Long INVALID_ID = -1L;
        private static final Long NON_EXISTENT_ID = 99L;

        @BeforeEach
        public void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(itemController)
                                .setControllerAdvice(new ValidationExceptionHandler())
                                .build();
                objectMapper = new ObjectMapper();

                testItem = new Item();
                testItem.setId(1L);
                testItem.setName("Test Item");
                testItem.setDescription("Test Description");
                testItem.setStatus("UNPROCESSED");
                testItem.setEmail("test@example.com");

                testItem2 = new Item();
                testItem2.setId(2L);
                testItem2.setName("Test Item 2");
                testItem2.setDescription("Test Description 2");
                testItem2.setStatus("UNPROCESSED");
                testItem2.setEmail("test2@example.com");

                testItem3 = new Item();
                testItem3.setId(3L);
                testItem3.setName("Test Item 3");
                testItem3.setDescription("Test Description 3");
                testItem3.setStatus("UNPROCESSED");
                testItem3.setEmail("test3@example.com");

                itemList = Arrays.asList(testItem, testItem2, testItem3);
        }

        @Test
        public void testGetAllItems_ReturnsAllItems() throws Exception {
                when(itemService.findAll()).thenReturn(itemList);

                mockMvc.perform(get("/api/items")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[2].id").value(3));

                verify(itemService, times(1)).findAll();
        }

        @Test
        public void testGetItemById_ValidId_ReturnsItem() throws Exception {
                when(itemService.findById(VALID_ID)).thenReturn(Optional.of(testItem));

                mockMvc.perform(get("/api/items/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("Test Item"));

                verify(itemService, times(1)).findById(1L);
        }

        @Test
        public void testGetItemById_InvalidId_ReturnsBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Id cannot be negative or zero"))
                                .when(itemService).findById(INVALID_ID);

                mockMvc.perform(get("/api/items/-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$[0].field").value("id"))
                                .andExpect(jsonPath("$[0].message").value("Id cannot be negative or zero"));
        }

        @Test
        public void testGetItemById_NonExistentId_ReturnsNotFound() throws Exception {
                when(itemService.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/items/99")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(itemService, times(1)).findById(99L);
        }

        @Test
        public void testCreateItem_ValidItem_ReturnsCreated() throws Exception {
                when(itemService.save(any(Item.class))).thenReturn(testItem);

                mockMvc.perform(post("/api/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testItem)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("Test Item"));

                verify(itemService, times(1)).save(any(Item.class));
        }

        @Test
        public void testCreateItem_NullItem_ReturnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                                .andExpect(status().isBadRequest());

                verify(itemService, never()).save(any(Item.class));
        }

        @Test
        public void testUpdateItem_ValidId_ReturnsOk() throws Exception {
                when(itemService.findById(VALID_ID)).thenReturn(Optional.of(testItem));
                when(itemService.save(any(Item.class))).thenReturn(testItem);

                mockMvc.perform(put("/api/items/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testItem)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(VALID_ID))
                                .andExpect(jsonPath("$.name").value("Test Item"));

                verify(itemService, times(1)).findById(VALID_ID);
                verify(itemService, times(1)).save(any(Item.class));
        }

        @Test
        public void testUpdateItem_InvalidId_ReturnsBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Id cannot be negative or zero"))
                                .when(itemService).findById(INVALID_ID);

                mockMvc.perform(put("/api/items/-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testItem)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$[0].field").value("id"))
                                .andExpect(jsonPath("$[0].message").value("Id cannot be negative or zero"));

                verify(itemService, times(1)).findById(INVALID_ID);
                verify(itemService, never()).save(any(Item.class));
        }

        @Test
        public void testUpdateItem_NonExistentId_ReturnsNotFound() throws Exception {
                when(itemService.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

                mockMvc.perform(put("/api/items/99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testItem)))
                                .andExpect(status().isNotFound());

                verify(itemService, times(1)).findById(NON_EXISTENT_ID);
                verify(itemService, never()).save(any(Item.class));
        }

        @Test
        public void testDeleteItem_ValidId_ReturnsNoContent() throws Exception {
                when(itemService.findById(VALID_ID)).thenReturn(Optional.of(testItem));
                doNothing().when(itemService).deleteById(VALID_ID);

                mockMvc.perform(delete("/api/items/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());

                verify(itemService, times(1)).findById(VALID_ID);
                verify(itemService, times(1)).deleteById(VALID_ID);
        }

        @Test
        public void testDeleteItem_InvalidId_ReturnsBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Id cannot be negative or zero"))
                                .when(itemService).findById(INVALID_ID);

                mockMvc.perform(delete("/api/items/-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$[0].field").value("id"))
                                .andExpect(jsonPath("$[0].message").value("Id cannot be negative or zero"));

                verify(itemService, never()).deleteById(anyLong());
        }

        @Test
        public void testDeleteItem_NonExistentId_ReturnsNotFound() throws Exception {
                when(itemService.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

                mockMvc.perform(delete("/api/items/99")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(itemService, times(1)).findById(NON_EXISTENT_ID);
                verify(itemService, never()).deleteById(NON_EXISTENT_ID);
        }

        @Test
        public void testProcessItems_ReturnsOk() throws Exception {
                CompletableFuture<List<Item>> future = CompletableFuture.completedFuture(itemList);
                when(itemService.processItemsAsync()).thenReturn(future);

                MvcResult mvcResult = mockMvc.perform(get("/api/items/process")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(request().asyncStarted())
                                .andReturn();

                mockMvc.perform(asyncDispatch(mvcResult))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[2].id").value(3));

                verify(itemService, times(1)).processItemsAsync();
        }

        @Test
        public void testProcessItems_WithException() throws Exception {
                CompletableFuture<List<Item>> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Processing failed"));
                when(itemService.processItemsAsync()).thenReturn(future);

                MvcResult mvcResult = mockMvc.perform(get("/api/items/process")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(request().asyncStarted())
                                .andReturn();

                mockMvc.perform(asyncDispatch(mvcResult))
                                .andExpect(status().isBadRequest());

                verify(itemService, times(1)).processItemsAsync();
        }
}
