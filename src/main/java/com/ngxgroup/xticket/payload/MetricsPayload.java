package com.ngxgroup.xticket.payload;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author bokon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricsPayload {

    private String name;
    private String description;
    private String baseUnit;
    private List<Measurement> measurements;
    private List<AvailableTag> availableTags;
}
