package com.sikza.apps.katatracker;

import com.sikza.apps.katatracker.data.Kata;
import com.sikza.apps.katatracker.data.repository.KataRepository;
import com.sikza.apps.katatracker.dto.ContentHolder;
import com.sikza.apps.katatracker.dto.GithubSearchResult;
import com.sikza.apps.katatracker.dto.kataInput;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/kata/api/")
@Transactional
public class Router {

    private final KataRepository kataRepository;
    private final R2dbcEntityTemplate entityTemplate;

    public Router(KataRepository kataRepository, R2dbcEntityTemplate entityTemplate) {
        this.kataRepository = kataRepository;
        this.entityTemplate = entityTemplate;
    }

    @GetMapping("/all")
    public Flux<Kata> all() {
        return kataRepository.findAll();
    }

    @PostMapping("/create")
    public Mono<Kata> create(kataInput input) {
        return entityTemplate.insert(new Kata(input.getName()));
    }

    @PostMapping("/delete/id/{id}")
    public Mono<Void> create(@PathVariable String id) {
        return kataRepository.deleteById(id);
    }

    @PostMapping("/test")
    public Mono<String> all1() {
        final WebClient client = WebClient.builder()
                .baseUrl("https://api.github.com")
                .build();

        String authToken = "Basic " + Base64Utils
                .encodeToString(("sikzatech@gmail.com" + ":" + "520bac8dac01685b37f67e0e9e6a7132f4a04129").getBytes(UTF_8));
        return client.get()
                .uri("/search/code?q=Main+filename:Main.java+repo:sikza/helloworld")
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(GithubSearchResult.class)
                .flatMap(result -> client
                        .get()
                        .uri(result.getItems().stream().findFirst().orElse(null).getUrl())
                        .header("Authorization", authToken)
                        .retrieve()
                        .bodyToMono(ContentHolder.class))
                .flatMap(data -> new StringDataCleaner().clean(data.getContent()))
                .flatMap(data -> Mono.just(StringUtils.trimAllWhitespace(new String(Base64.getDecoder().decode(data)))));
    }
}
