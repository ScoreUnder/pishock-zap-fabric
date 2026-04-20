package moe.score.pishockzap.util;

import org.apache.commons.lang3.tuple.Pair;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class URIBuilder {
    private final URI baseUri;
    private final ArrayList<Pair<String, String>> queryParams = new ArrayList<>();

    public URIBuilder(URI baseUri) {
        this.baseUri = baseUri;
    }

    public URIBuilder(String baseUriStr) {
        this.baseUri = URI.create(baseUriStr);
    }

    public URIBuilder addParameter(String name, String value) {
        queryParams.add(Pair.of(name, value));
        return this;
    }

    public URI build() {
        return of(baseUri, queryParams);
    }

    public static URI of(URI base, List<Pair<String, String>> queryParams) {
        if (queryParams.isEmpty()) return base;
        try {

            var oldQuery = base.getQuery();
            var newQuery = queryParams.stream().map(p ->
                    URLEncoder.encode(p.getLeft(), StandardCharsets.UTF_8) +
                        "=" + URLEncoder.encode(p.getRight(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
            var fullQuery = (oldQuery == null || oldQuery.isEmpty()) ? newQuery : oldQuery + "&" + newQuery;
            return new URI(base.getScheme(), base.getAuthority(), base.getPath(), fullQuery, base.getFragment());
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
