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

    public String name;
    public String description;
    public String baseUnit;
    public List<Measurement> measurements;
    public List<AvailableTag> availableTags;
}
