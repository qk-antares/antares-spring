package com.antares.scan;

import com.antares.imported.LocalDateConfiguration;
import com.antares.imported.ZonedDateConfiguration;
import com.antares.spring.annotation.ComponentScan;
import com.antares.spring.annotation.Import;

@ComponentScan
@Import({LocalDateConfiguration.class, ZonedDateConfiguration.class})
public class ScanApplication {
    
}
