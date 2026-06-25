package wwn.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wwn.backend.dto.response.ArticleResponse;
import wwn.backend.service.BookmarkService;

import java.util.List;

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

    /**
     * 북마크 조회
     */
    @GetMapping("/list")
    public ResponseEntity<List<ArticleResponse>> getBookmarkedArticles() {
        List<ArticleResponse> result = bookmarkService.getBookmarkedArticles();
        return ResponseEntity.ok(result);
    }
}

