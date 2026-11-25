# Shader Walls — خلفيات حية للهاتف

تطبيق أندرويد فيه 16 خلفية حية متحركة بالطول (Bricks، ASCII Terminal، Mosaic، Amethyst Tiles، Dot Screen، Glitch Blinds، Glass Tiles، Liquid Light، Lavender Weave، Hex Beads، LED Matrix، Sunburst، Violet Glass، Interference، Aura، Light Web).
كل خلفية مرسومة بالـ OpenGL مباشرة على الهاتف، لذلك تتحرك بسلاسة وتبقى حادة على أي دقة شاشة.

## الحصول على ملف APK من الهاتف (بدون كمبيوتر)
1. أنشئ حساباً على github.com وأنشئ مستودعاً جديداً (New repository).
2. ارفع محتويات هذا المجلد كما هي (Add file ← Upload files)، مع مجلد `.github` المخفي.
   - إذا لم يقبل المتصفح المجلد المخفي: أنشئ ملفاً جديداً باسم `.github/workflows/build-apk.yml` والصق فيه محتوى الملف.
3. افتح تبويب **Actions**؛ سيبدأ البناء تلقائياً (حوالي 3–5 دقائق).
4. عند ظهور ✅ افتح البناء وحمّل **ShaderWalls-apk** من قسم Artifacts، ثم فك الضغط وثبّت `ShaderWalls.apk`.

## عن طريق Android Studio
افتح المجلد في Android Studio ثم: Build ← Build APK(s).

## الاستخدام
افتح التطبيق، اختر الأسلوب من الشبكة، ثم اضغط «تعيين كخلفية».
عند تغيير الأسلوب من داخل التطبيق تتغير الخلفية الحالية فوراً.

## إضافة أسلوب جديد
أضف ملف `.frag` في `app/src/main/assets/shaders/` ثم أضف اسمه في `Styles.java`.
