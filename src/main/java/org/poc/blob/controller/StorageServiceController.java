package org.poc.blob.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.poc.blob.service.StorageItem;
import org.poc.blob.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
public class StorageServiceController {

    private final StorageService storageService;

    @Autowired
    public StorageServiceController(final StorageService storageService) {
        this.storageService = storageService;
        this.storageService.init();
    }

    @GetMapping(value = "/")
    public ResponseEntity<Map<String, Object>> listUploadedFiles(final Model model,
                                                                 final HttpServletResponse response) {
        model.addAttribute("files", storageService.listAllFiles().collect(Collectors.toList()));
        return new ResponseEntity<>(model.asMap(), HttpStatus.OK);
    }

    @GetMapping(value = "/modelToJson", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getModelAsJson(Model model) {
        // Adding attributes to the model
        model.addAttribute("name", "John Doe");
        model.addAttribute("age", 30);

        // Convert Model to Map
        Map<String, Object> response = model.asMap();

        // Return ResponseEntity with JSON and HTTP status
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable final String filename) {
        final StorageItem storageItem = storageService.getFile(filename);

        final String contentDisposition;
        switch (storageItem.getContentDisplayMode()) {
            default:
            case DOWNLOAD: {
                contentDisposition = "attachment";
                break;
            }
            case MODAL_POPUP:
            case NEW_BROWSER_TAB: {
                contentDisposition = "inline";
            }
        }

        final Resource body = new InputStreamResource(storageItem.getContent(), storageItem.getFileName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition + "; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(storageItem.getContentType()))
                .body(body);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") final MultipartFile file,
                                   final RedirectAttributes redirectAttributes) {
        boolean success = false;
        try {
            storageService.store(file.getOriginalFilename(), file.getInputStream(), file.getSize());
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        redirectAttributes.addFlashAttribute("success", success);
        redirectAttributes.addFlashAttribute("message", success ?
                "You successfully uploaded " + file.getOriginalFilename() + "!" :
                "Failed to upload " + file.getOriginalFilename());

        return "redirect:/";
    }

    @GetMapping("/files/delete/{filename}")
    public String deleteFile(@PathVariable final String filename,
                             final RedirectAttributes redirectAttributes) {
        final boolean success = storageService.deleteFile(filename);

        redirectAttributes.addFlashAttribute("success", success);
        redirectAttributes.addFlashAttribute("message", success ?
                "You successfully deleted " + filename + "!" :
                "Failed to delete " + filename + ".");

        return "redirect:/";
    }
}