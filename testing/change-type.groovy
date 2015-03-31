AnnotationSet mentions = doc.getAnnotations("LearningFramework").get("Mention")
    for(mention in mentions){
        FeatureMap fm = mention.getFeatures()
        fm.put("type", fm.get("LF_class"))
    }
