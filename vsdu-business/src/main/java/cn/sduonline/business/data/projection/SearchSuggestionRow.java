package cn.sduonline.business.data.projection;

import lombok.Data;

@Data
public class SearchSuggestionRow {
    private String type;
    private Long id;
    private String text;
    private String subtitle;
}
