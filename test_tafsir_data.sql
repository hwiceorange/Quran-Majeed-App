-- ===================================
-- 印尼语 Tafsir 测试数据
-- ===================================
-- 用于测试 Tafsir API
-- 包含前 3 章的部分经文注释

-- 第 1 章 Al-Fatihah (开端章)
INSERT INTO `tafsir_indonesian` (`surah_id`, `ayat_id`, `text`, `language`) VALUES
(1, 1, 'Bismillahirrahmanirrahim artinya "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang." Ini adalah permulaan dari segala kebaikan dan keberkahan.', 'id'),
(1, 2, 'Alhamdulillahi rabbil \'alamin. Segala puji bagi Allah, Tuhan seluruh alam. Ini mengajarkan kita untuk selalu bersyukur kepada Allah atas segala nikmat yang diberikan.', 'id'),
(1, 3, 'Ar-Rahmanir-Rahim. Yang Maha Pengasih, Maha Penyayang. Allah memberikan rahmat dan kasih sayang-Nya kepada semua makhluk.', 'id'),
(1, 4, 'Maalikiyaumiddin. Pemilik hari pembalasan. Di hari kiamat, semua amal perbuatan akan dipertanggungjawabkan.', 'id'),
(1, 5, 'Iyyaka na\'budu wa iyyaka nasta\'in. Hanya kepada-Mu kami menyembah dan hanya kepada-Mu kami memohon pertolongan.', 'id'),
(1, 6, 'Ihdinash shirathal mustaqim. Tunjukilah kami jalan yang lurus, yaitu jalan yang diridhai Allah.', 'id'),
(1, 7, 'Shirathal ladhina an\'amta alaihim. Jalan orang-orang yang telah Engkau beri nikmat, bukan jalan mereka yang dimurkai dan bukan pula jalan mereka yang sesat.', 'id');

-- 第 2 章 Al-Baqarah (黄牛章)
INSERT INTO `tafsir_indonesian` (`surah_id`, `ayat_id`, `text`, `language`) VALUES
(2, 1, 'Alif Lam Mim. Ini adalah huruf-huruf muqatha\'ah yang hanya Allah yang mengetahui maksudnya.', 'id'),
(2, 2, 'Dzalikal kitabu la raiba fih. Kitab (Al-Quran) ini tidak ada keraguan padanya, petunjuk bagi mereka yang bertakwa.', 'id'),
(2, 255, 'Ayat Al-Kursi adalah ayat terbesar dalam Al-Quran. Allah tidak mengantuk dan tidak tidur. Kepunyaan-Nya apa yang ada di langit dan di bumi. Kursi-Nya meliputi langit dan bumi, dan Dia tidak merasa berat memelihara keduanya.', 'id');

-- 第 3 章 Ali Imran (伊姆兰的家属)
INSERT INTO `tafsir_indonesian` (`surah_id`, `ayat_id`, `text`, `language`) VALUES
(3, 1, 'Alif Lam Mim. Huruf-huruf ini adalah rahasia Allah dalam Al-Quran.', 'id'),
(3, 2, 'Allah, tidak ada Tuhan selain Dia. Yang Hidup terus-menerus, Yang mengurus segala sesuatu.', 'id'),
(3, 3, 'Dia menurunkan Kitab (Al-Quran) kepadamu (Muhammad) dengan sebenarnya, membenarkan kitab-kitab yang diturunkan sebelumnya, dan menurunkan Taurat dan Injil.', 'id');

