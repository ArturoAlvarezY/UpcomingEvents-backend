package org.zoodevelopers.upcoming_events.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.zoodevelopers.upcoming_events.exceptions.IOException;
import org.zoodevelopers.upcoming_events.exceptions.StorageFileNotFoundException;
import org.zoodevelopers.upcoming_events.services.implementation.IStorageService;
import org.springframework.http.HttpHeaders;

import java.util.stream.Collectors;
import org.springframework.core.io.Resource;

@RestController
public class FileUploaderController {

    private  IStorageService storageService;

    @Autowired
    public void FileUploadController(IStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {

        model.addAttribute("files", storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(FileUploaderController.class,
                        "serveFile", path.getFileName().toString()).build().toUri().toString())
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);

        if (file == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("files") MultipartFile[] files,
            RedirectAttributes redirectAttributes) {

        for (MultipartFile file : files) {
            storageService.store(file);
        }

        StringBuilder message = new StringBuilder("You successfully uploaded: ");
        for (MultipartFile file : files) {
            message.append(file.getOriginalFilename()).append(", ");
        }

        if (message.length() > 0) {
            message.setLength(message.length() - 2);
        }

        redirectAttributes.addFlashAttribute("message", message.toString() + "!");

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
