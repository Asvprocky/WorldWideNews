package wwn.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wwn.backend.service.BookmarkService;

@RestController
@RequestMapping("/bookmark")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * 북마크 토글 (등록 / 취소)
     * POST /api/bookmarks?articleId=123
     */
    @PostMapping
    public ResponseEntity<String> toggleBookmark(@RequestParam Long articleId) {
        String result = bookmarkService.toggleBookmark(articleId);
        return ResponseEntity.ok(result);
    }
}
