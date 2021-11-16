package cybercubeChallenge.ui.common;

import lombok.Data;

@Data
public class PagesConfig {

    private String vacancies;

    public PagesConfig() {
        ReadConfig readConfig = new ReadConfig();
        vacancies = readConfig.getPropValue("pages.vacancies");
    }
}
