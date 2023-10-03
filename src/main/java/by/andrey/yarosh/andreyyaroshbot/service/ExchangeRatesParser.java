package by.andrey.yarosh.andreyyaroshbot.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ExchangeRatesParser {

    private final String url =  "https://www.belta.by/currency/";

    public String ratesParser(){
        Document doc = null;
        String exchangeRates = null;
        try {
            doc = Jsoup.connect(url).
                    timeout(5000).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Elements element = doc.getElementsByClass("sp_rubric_title sp_title_shot with_rubrics");
        exchangeRates = element.text();
        return exchangeRates;
    }
}
