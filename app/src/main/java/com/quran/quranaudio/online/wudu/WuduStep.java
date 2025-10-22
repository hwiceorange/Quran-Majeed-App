package com.quran.quranaudio.online.wudu;

/**
 * Data model for a Wudu step
 */
public class WuduStep {
    private int stepNumber;
    private String title;
    private String titleArabic;
    private String description;
    private String descriptionArabic;
    private String imageName;
    private String dua;
    private String duaArabic;
    private String duaTransliteration;

    public WuduStep(int stepNumber, String title, String titleArabic, 
                    String description, String descriptionArabic, String imageName) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.titleArabic = titleArabic;
        this.description = description;
        this.descriptionArabic = descriptionArabic;
        this.imageName = imageName;
    }

    public WuduStep(int stepNumber, String title, String titleArabic, 
                    String description, String descriptionArabic, String imageName,
                    String dua, String duaArabic, String duaTransliteration) {
        this(stepNumber, title, titleArabic, description, descriptionArabic, imageName);
        this.dua = dua;
        this.duaArabic = duaArabic;
        this.duaTransliteration = duaTransliteration;
    }

    // Getters
    public int getStepNumber() { 
        return stepNumber; 
    }
    
    public String getTitle() { 
        return title; 
    }
    
    public String getTitleArabic() { 
        return titleArabic; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public String getDescriptionArabic() { 
        return descriptionArabic; 
    }
    
    public String getImageName() { 
        return imageName; 
    }
    
    public String getDua() { 
        return dua; 
    }
    
    public String getDuaArabic() { 
        return duaArabic; 
    }
    
    public String getDuaTransliteration() { 
        return duaTransliteration; 
    }
    
    public boolean hasDua() { 
        return dua != null && !dua.isEmpty(); 
    }
}

