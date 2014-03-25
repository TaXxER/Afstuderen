setwd("C:/Git-data/Afstuderen/method_selection")
ltr_data = read.csv("ndcg10_results.csv")
temp <- ltr_data[,c(2:(length(ltr_data)-2))]
winnum <- apply(temp, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{sum(x[!is.na(x)]<y)}))