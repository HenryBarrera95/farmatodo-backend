package com.farmatodo.token;

import com.farmatodo.token.dto.CreateTokenRequest;
import com.farmatodo.token.dto.CreateTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tokens")
@Validated
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<CreateTokenResponse> createToken(@Valid @RequestBody CreateTokenRequest req) {
        var entity = tokenService.createToken(req);
        return ResponseEntity.ok(new CreateTokenResponse(entity.getToken(), entity.getMaskedPan()));
    }
}
