package com.night_club.bar_restaurant.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DarajaCallbackPayload(
        @JsonProperty("Body") Body body
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonProperty("stkCallback") StkCallback stkCallback
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StkCallback(
            @JsonProperty("MerchantRequestID") String merchantRequestId,
            @JsonProperty("CheckoutRequestID") String checkoutRequestId,
            @JsonProperty("ResultCode") Integer resultCode,
            @JsonProperty("ResultDesc") String resultDesc,
            // Absent entirely when ResultCode != 0 (payment failed/cancelled) -
            // must be nullable, never assume it's present.
            @JsonProperty("CallbackMetadata") CallbackMetadata callbackMetadata
    ) {
        public boolean isSuccess() {
            return resultCode != null && resultCode == 0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackMetadata(
            @JsonProperty("Item") List<CallbackItem> item
    ) {
    }

    // Value can be a String, Integer, or Double depending on the field
    // (MpesaReceiptNumber is a string, Amount/PhoneNumber are numbers) -
    // deserialize as Object and cast/convert per-field when reading.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackItem(
            @JsonProperty("Name") String name,
            @JsonProperty("Value") Object value
    ) {
    }
}