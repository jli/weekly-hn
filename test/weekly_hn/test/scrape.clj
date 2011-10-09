(ns weekly-hn.test.scrape
  (:use [weekly-hn.scrape])
  (:use [clojure.test]))

(def stories1
     [{:id 3085518, :link "http://techcrunch.com/2011/10/07/steve-jobs-2", :title "Colbert tribute to Steve Jobs", :points 231, :user "jasontan", :comments 26}
      {:id 3086320, :link "http://type.method.ac", :title "Show HN: KernType, a typography game made with Raphaël", :points 34, :user "duopixel", :comments 8}
      {:id 3085004, :link "http://arstechnica.com/business/news/2011/10/exclusive-computer-virus-hits-drone-fleet.ars", :title "Computer virus hits US Predator and Reaper drone fleet", :points 263, :user "llambda", :comments 145}
      {:id 3085855, :link "http://www.avichal.com/2011/10/07/why-education-startups-do-not-succeed/", :title "Why Education Companies Do Not Succeed", :points 92, :user "avichal", :comments 22}
      {:id 3085537, :link "http://bitquabit.com/post/enslaving-your-interns-for-evil-and-profit/", :title "Enslaving your interns for evil and profit", :points 84, :user "tghw", :comments 46}
      {:id 3083935, :link "https://github.com/waseems/inbox2_desktop", :title "Tell HN: I just open-sourced the e-mail client I spent 2 years of my life on", :points 360, :user "waseemsadiq", :comments 103}
      {:id 3085477, :link "http://www.b3ta.com/board/10563417/", :title "Breaking a MacBook in memory of Steve Jobs", :points 86, :user "hackermom", :comments 6}
      {:id 3083770, :link "http://tumblr.amitgupta.com/post/11102689089/two-weeks-ago-i-got-a-call-from-my-doctor-who-id", :title "Photojojo, Jelly founder needs bone marrow match to save his life", :points 362, :user "ck2", :comments 65}
      {:id 3086066, :link "http://www.betabeat.com/2011/10/07/google-sues-itself-with-a-little-help-from-intellectual-ventures/", :title "Google Sues Itself With Help From Intellectual Ventures", :points 27, :user "padrack", :comments 15}
      {:id 3085534, :link "http://arstechnica.com/tech-policy/news/2011/10/worlds-leading-patent-troll-sues-motorola.ars", :title "World's leading patent troll sues Motorola over Android phones", :points 59, :user "bryanlarsen", :comments 20}
      {:id 3084916, :link "http://www.kickstarter.com/projects/peterseid/romo-the-smartphone-robot", :title "Turn Your Smartphone Into A Robot", :points 109, :user "whather", :comments 29}
      {:id 3083410, :link "http://www.smashingmagazine.com/2011/10/07/16-pixels-body-copy-anything-less-costly-mistake/", :title "16 pixels For Body Copy. Anything Less Is A Costly Mistake", :points 307, :user "m1nes", :comments 116}
      {:id 3084961, :link "http://www.nytimes.com/2011/10/06/opinion/jobs-looked-to-the-future.html", :title "Against Nostalgia - Mike Daisey on Steve Jobs", :points 87, :user "bgruber", :comments 35}
      {:id 3078128, :link "http://www.apple.com/stevejobs/", :title "Steve Jobs has passed away.", :points 3754, :user "patricktomas", :comments 356}
      {:id 3086284, :link "http://baydin.com/blog/2011/10/when-software-jumps-the-shark/", :title "What Arrested Development and Successful products have in common?", :points 10, :user "moah", :comments 2}
      {:id 3086277, :link "http://daringfireball.net/2011/10/thoughts_and_observations_iphone_4s", :title "Thoughts and Observations Regarding This Week’s iPhone 4S Event", :points 9, :user "technoslut", :comments 1}
      {:id 3085570, :link "http://techcrunch.com/2011/10/07/confirmed-pinterest-raises-27-million-round-led-by-andreessen-horowitz/", :title "Pinterest raises $27 million in a round led by Andreessen Horowitz", :points 32, :user "noelsequeira", :comments 25}
      {:id 3084555, :link "http://lists.cs.uiuc.edu/pipermail/llvmdev/2011-October/043719.html", :title "LLVM IR is a compiler IR", :points 81, :user "pcwalton", :comments 33}
      {:id 3085660, :link "http://www.law.com/jsp/cc/PubArticleCC.jsp?id=1202518061641&Texas_Judge_Leaves_His_Patent_Rocket_Docket_to_Practice_Law", :title "Texas Judge Leaves His Patent 'Rocket Docket' to Practice Law", :points 24, :user "sehugg", :comments 10}
      {:id 3086218, :link "http://www.nytimes.com/1997/09/01/business/an-unknown-co-founder-leaves-after-20-years-of-glory-and-turmoil.html?pagewanted=all&src=pm", :title "An 'Unknown' Co-Founder Leaves After 20 Years of Glory and Turmoil (1997) ", :points 9, :user "sinzone", :comments 4}
      {:id 3085228, :link "http://arstechnica.com/business/news/2011/10/google-puts-mysql-in-app-engine-cloud.ars", :title "Google puts MySQL in App Engine Cloud with Google Cloud SQL", :points 38, :user "benjaminfox", :comments 10}
      {:id 3084487, :link "http://blogs.msdn.com/b/b8/archive/2011/10/07/reducing-runtime-memory-in-windows-8.aspx", :title "Reducing runtime memory in Windows 8", :points 68, :user "ghurlman", :comments 18}
      {:id 3084195, :link "http://www.catonmat.net/blog/worlds-best-introduction-to-sed/", :title "Introduction to sed", :points 89, :user "pkrumins", :comments 25}
      {:id 3083354, :link "http://www.h3rald.com/articles/10-programming-languages/", :title "Programming languages worth checking out", :points 169, :user "bleakgadfly", :comments 45}
      {:id 3084283, :link "http://www.pcworld.com/article/241160/kickstarter_faces_patent_suit_over_funding_idea.html", :title "Kickstarter being sued for patent infringment.", :points 71, :user "Unregistered", :comments 39}
      {:id 3082383, :link "http://hhn.domador.net/2011/10/05/23/", :title "Hacker News Front Page Snapshot from Last Night", :points 483, :user "dstein64", :comments 146}
      {:id 3085495, :link "http://hardware.slashdot.org/story/11/10/07/0344214/hp-to-introduce-flash-memory-replacement-in-2013", :title "HP To Introduce Flash Memory Replacement In 2013", :points 27, :user "modeless", :comments 13}
      {:id 3084252, :link "http://www.marco.org/2011/10/07/review-79-kindle-with-ads-and-buttons", :title "Review: The 2011 $79 Kindle with ads and buttons", :points 77, :user "revorad", :comments 81}
      {:id 3085837, :link "http://www.itworld.com/mobile-wireless/211341/voice-recognition-software-vendor-nuance-buys-rival-swype", :title "Voice recognition software vendor Nuance buys rival Swype", :points 12, :user "junioreven", :comments 1}
      {:id 3083828, :link "http://www.extremetech.com/extreme/98941-graphene-creates-electricity-when-struck-by-light", :title "Graphene creates electricity when struck by light", :points 81, :user "kennjason", :comments 15}])

(def stories2
     [{:id 3085518, :link "http://techcrunch.com/2011/10/07/steve-jobs-2", :title "Colbert tribute to Steve Jobs", :points 233, :user "jasontan", :comments 26}
      {:id 3086320, :link "http://type.method.ac", :title "Show HN: KernType, a typography game made with Raphaël", :points 41, :user "duopixel", :comments 11}
      {:id 3085004, :link "http://arstechnica.com/business/news/2011/10/exclusive-computer-virus-hits-drone-fleet.ars", :title "Computer virus hits US Predator and Reaper drone fleet", :points 264, :user "llambda", :comments 146}
      {:id 3085855, :link "http://www.avichal.com/2011/10/07/why-education-startups-do-not-succeed/", :title "Why Education Companies Do Not Succeed", :points 94, :user "avichal", :comments 23}
      {:id 3085537, :link "http://bitquabit.com/post/enslaving-your-interns-for-evil-and-profit/", :title "Enslaving your interns for evil and profit", :points 84, :user "tghw", :comments 46}
      {:id 3083935, :link "https://github.com/waseems/inbox2_desktop", :title "Tell HN: I just open-sourced the e-mail client I spent 2 years of my life on", :points 360, :user "waseemsadiq", :comments 104}
      {:id 3085477, :link "http://www.b3ta.com/board/10563417/", :title "Breaking a MacBook in memory of Steve Jobs", :points 89, :user "hackermom", :comments 6}
      {:id 3083770, :link "http://tumblr.amitgupta.com/post/11102689089/two-weeks-ago-i-got-a-call-from-my-doctor-who-id", :title "Photojojo, Jelly founder needs bone marrow match to save his life", :points 363, :user "ck2", :comments 65}
      {:id 3086066, :link "http://www.betabeat.com/2011/10/07/google-sues-itself-with-a-little-help-from-intellectual-ventures/", :title "Google Sues Itself With Help From Intellectual Ventures", :points 27, :user "padrack", :comments 17}
      {:id 3085534, :link "http://arstechnica.com/tech-policy/news/2011/10/worlds-leading-patent-troll-sues-motorola.ars", :title "World's leading patent troll sues Motorola over Android phones", :points 59, :user "bryanlarsen", :comments 20}
      {:id 3084916, :link "http://www.kickstarter.com/projects/peterseid/romo-the-smartphone-robot", :title "Turn Your Smartphone Into A Robot", :points 109, :user "whather", :comments 29}
      {:id 3083410, :link "http://www.smashingmagazine.com/2011/10/07/16-pixels-body-copy-anything-less-costly-mistake/", :title "16 pixels For Body Copy. Anything Less Is A Costly Mistake", :points 307, :user "m1nes", :comments 116}
      {:id 3086277, :link "http://daringfireball.net/2011/10/thoughts_and_observations_iphone_4s", :title "Thoughts and Observations Regarding This Week’s iPhone 4S Event", :points 11, :user "technoslut", :comments 2}
      {:id 3084961, :link "http://www.nytimes.com/2011/10/06/opinion/jobs-looked-to-the-future.html", :title "Against Nostalgia - Mike Daisey on Steve Jobs", :points 88, :user "bgruber", :comments 35}
      {:id 3078128, :link "http://www.apple.com/stevejobs/", :title "Steve Jobs has passed away.", :points 3755, :user "patricktomas", :comments 356}
      {:id 3085570, :link "http://techcrunch.com/2011/10/07/confirmed-pinterest-raises-27-million-round-led-by-andreessen-horowitz/", :title "Pinterest raises $27 million in a round led by Andreessen Horowitz", :points 33, :user "noelsequeira", :comments 26}
      {:id 3086284, :link "http://baydin.com/blog/2011/10/when-software-jumps-the-shark/", :title "What Arrested Development and Successful products have in common?", :points 10, :user "moah", :comments 2}
      {:id 3084555, :link "http://lists.cs.uiuc.edu/pipermail/llvmdev/2011-October/043719.html", :title "LLVM IR is a compiler IR", :points 81, :user "pcwalton", :comments 33}
      {:id 3085660, :link "http://www.law.com/jsp/cc/PubArticleCC.jsp?id=1202518061641&Texas_Judge_Leaves_His_Patent_Rocket_Docket_to_Practice_Law", :title "Texas Judge Leaves His Patent 'Rocket Docket' to Practice Law", :points 24, :user "sehugg", :comments 10}
      {:id 3086218, :link "http://www.nytimes.com/1997/09/01/business/an-unknown-co-founder-leaves-after-20-years-of-glory-and-turmoil.html?pagewanted=all&src=pm", :title "An 'Unknown' Co-Founder Leaves After 20 Years of Glory and Turmoil (1997) ", :points 9, :user "sinzone", :comments 4}
      {:id 3085228, :link "http://arstechnica.com/business/news/2011/10/google-puts-mysql-in-app-engine-cloud.ars", :title "Google puts MySQL in App Engine Cloud with Google Cloud SQL", :points 39, :user "benjaminfox", :comments 10}
      {:id 3084487, :link "http://blogs.msdn.com/b/b8/archive/2011/10/07/reducing-runtime-memory-in-windows-8.aspx", :title "Reducing runtime memory in Windows 8", :points 70, :user "ghurlman", :comments 18}
      {:id 3084195, :link "http://www.catonmat.net/blog/worlds-best-introduction-to-sed/", :title "Introduction to sed", :points 89, :user "pkrumins", :comments 25}
      {:id 3084283, :link "http://www.pcworld.com/article/241160/kickstarter_faces_patent_suit_over_funding_idea.html", :title "Kickstarter being sued for patent infringment.", :points 72, :user "Unregistered", :comments 39}
      {:id 3086400, :link "https://github.com/hjwp/Test-Driven-Django-Tutorial", :title "A rewrite of the Django tutorial with Test Driven Development", :points 4, :user "rbanffy", :comments 0}
      {:id 3082383, :link "http://hhn.domador.net/2011/10/05/23/", :title "Hacker News Front Page Snapshot from Last Night", :points 483, :user "dstein64", :comments 146}
      {:id 3083354, :link "http://www.h3rald.com/articles/10-programming-languages/", :title "Programming languages worth checking out", :points 170, :user "bleakgadfly", :comments 45}
      {:id 3085837, :link "http://www.itworld.com/mobile-wireless/211341/voice-recognition-software-vendor-nuance-buys-rival-swype", :title "Voice recognition software vendor Nuance buys rival Swype", :points 14, :user "junioreven", :comments 1}
      {:id 3084252, :link "http://www.marco.org/2011/10/07/review-79-kindle-with-ads-and-buttons", :title "Review: The 2011 $79 Kindle with ads and buttons", :points 77, :user "revorad", :comments 81}
      {:id 3085495, :link "http://hardware.slashdot.org/story/11/10/07/0344214/hp-to-introduce-flash-memory-replacement-in-2013", :title "HP To Introduce Flash Memory Replacement In 2013", :points 27, :user "modeless", :comments 13}])

(def stories3
     [{:id 3089010, :link "http://jng.imagine27.com/articles/2011-10-02-171602_overtone.html", :title "Overtone", :points 253, :user "wglb", :comments 45}
      {:id 3089352, :link "http://www.nytimes.com/2011/10/09/sunday-review/coming-soon-the-drone-arms-race.html", :title "Coming Soon: the Drone Arms Race", :points 41, :user "OstiaAntica", :comments 28}
      {:id 3088687, :link "http://www.ccc.de/en/updates/2011/staatstrojaner", :title "German Chaos Computer Club analyzes and releases government malware", :points 221, :user "venti", :comments 32}
      {:id 3089577, :link "http://www.reddit.com/tb/l51bb", :title "Customs Form filled by Apollo 11", :points 11, :user "azal", :comments 0}
      {:id 3089355, :link "http://www.markdrop.com/", :title "Show HN: Markdrop — drag and drop Markdown previews", :points 29, :user "__init__py", :comments 6}
      {:id 3088382, :link "http://bhorowitz.com/2011/10/08/nobody-cares/", :title "Nobody Cares", :points 161, :user "timf", :comments 29}
      {:id 3088901, :link "http://www.thice.nl/hide-your-data-in-plain-sight-usb-hardware-hiding/", :title "Hiding your data in plain sight: USB hardware hiding", :points 59, :user "chaosmachine", :comments 15}
      {:id 3088739, :link "http://nextbigfuture.com/2011/10/hp-plans-to-release-first-memristor.html", :title "HP plans to release first memristor, alternative to flash and SSDs in 18 months", :points 92, :user "peritpatrio", :comments 26}
      {:id 3089451, :link "https://plus.google.com/112374836634096795698/posts/8cfpr9k5v6t", :title "Guy Kawasaki: What I learned from Steve Jobs", :points 15, :user "dm8", :comments 0}
      {:id 3088290, :link "http://projects.mikewest.org/vimroom/", :title "Vimroom", :points 127, :user "drKarl", :comments 35}
      {:id 3088295, :link "http://lostinjit.blogspot.com/2011/10/pypys-future-directions.html", :title "PyPy's future directions", :points 110, :user "jemeshsu", :comments 7}
      {:id 3089244, :link "http://jsonk.posterous.com/linkedin-product-manager-interview-review", :title "LinkedIn Product Manager interview review", :points 15, :user "jsnk", :comments 15}
      {:id 3088671, :link "http://www.greens-efa.eu/fileadmin/dam/Documents/Policy_papers/Common%20position%20on%20copyright%2028sept11_EN.pdf", :title "EU Green Party adopts the Pirate Party's position on copyright", :points 66, :user "sp332", :comments 12}
      {:id 3078128, :link "http://www.apple.com/stevejobs/", :title "Steve Jobs has passed away.", :points 4015, :user "patricktomas", :comments 359}
      {:id 3089336, :link "https://mix.oracle.com/home", :title "Oracle launches their own social network", :points 9, :user "cleverjake", :comments 12}
      {:id 3087492, :link "http://gamasutra.com/view/news/37762/Steve_Jobs_Atari_Employee_Number_40.php", :title "\"He offered the Apple II to Atari... we said no. No thank you.\"", :points 222, :user "doomlaser", :comments 47}
      {:id 3089158, :link "http://shape-of-code.coding-guidelines.com/2011/10/08/memory-capacity-and-commercial-compiler-development/", :title "Memory capacity and commercial compiler development", :points 16, :user "johndcook", :comments 8}
      {:id 3088284, :link "http://xenonauts.com/component/content/article/1-latest-news/121-pre-order-issues", :title "Pre-orders are scams according to Paypal", :points 67, :user "dmak", :comments 40}
      {:id 3089019, :link "https://github.com/WebReflection/JSONH", :title "JSONH: JSON Homogeneous Collections Compressor", :points 21, :user "d0vs", :comments 8}
      {:id 3089259, :link "item?id=3089259", :title "Ask HN: What are your music prototyping solutions?", :points 33, :user "przemoc", :comments 10}
      {:id 3087322, :link "http://www.reddit.com/r/reddit.com/comments/l4q2y/please_help_me_expose_this_newest_paypal_fraud/", :title "Redditor burnt by Paypal's \"Rolling Reserve\" feature", :points 221, :user "mwill", :comments 53}
      {:id 3087932, :link "http://www.pcpro.co.uk/news/broadband/370393/isps-exaggerate-the-cost-of-data", :title "ISPs \"exaggerate the cost of data\"", :points 89, :user "pwg", :comments 38}
      {:id 3088602, :link "http://www.tomsguide.com/us/application-purchase-ecommerce-advertising-ios-android-blackberry-windows-phone-lodsys,news-12806.html", :title "Apple Files Patent for In-App Purchases", :points 38, :user "grellas", :comments 26}
      {:id 3087314, :link "https://chrome.google.com/webstore/detail/gbchcmhmhahfdphkhkmpfmihenigjmpp", :title "Chrome Remote Desktop extension by Google", :points 199, :user "patrickaljord", :comments 34}
      {:id 3087969, :link "http://rjlipton.wordpress.com/2011/10/08/an-annoying-open-problem/", :title "An Annoying Open Problem ", :points 80, :user "wglb", :comments 12}
      {:id 3088650, :link "http://singularityhub.com/2011/10/08/robot-i-now-have-common-sense-engineer-great-go-fetch-me-a-sandwich/", :title "Robot: I Now Have Common Sense. Engineer: Great, Go Fetch Me a Sandwich", :points 40, :user "ph0rque", :comments 20}
      {:id 3085518, :link "http://techcrunch.com/2011/10/07/steve-jobs-2", :title "Colbert tribute to Steve Jobs", :points 448, :user "jasontan", :comments 50}
      {:id 3088860, :link "http://www.craig-wood.com/nick/articles/pi-chudnovsky/", :title "100,000,000 million places of π in just under 10 minutes (with Python)", :points 27, :user "reidrac", :comments 6}
      {:id 3086793, :link "http://larrythefreesoftwareguy.wordpress.com/2011/10/07/time-to-fork-the-fsf/", :title "Time to fork the FSF", :points 316, :user "pjhyett", :comments 279}
      {:id 3086866, :link "http://www.nytimes.com/2011/10/07/opinion/the-man-who-inspired-jobs.html?hp", :title "The Man Who Inspired Jobs", :points 213, :user "wallflower", :comments 14}])

(def small-stories1
     [{:id 3082383, :title "Hacker News Front Page Snapshot from Last Night", :points 483}
      {:id 3085495, :title "HP To Introduce Flash Memory Replacement In 2013", :points 27}
      {:id 3084252, :title "Review: The 2011 $79 Kindle with ads and buttons", :points 77}
      {:id 3085837, :title "Voice recognition software vendor Nuance buys rival Swype", :points 12}
      {:id 3083828, :title "Graphene creates electricity when struck by light", :points 81}])

(def small-stories2
     [{:id 3082383, :title "Hacker News Front Page Snapshot from Last Night", :points 483}
      {:id 3083354, :title "Programming languages worth checking out", :points 170}
      {:id 3085837, :title "Voice recognition software vendor Nuance buys rival Swype", :points 14}
      {:id 3084252, :title "Review: The 2011 $79 Kindle with ads and buttons", :points 77}
      {:id 3085495, :title "HP To Introduce Flash Memory Replacement In 2013", :points 27}])

(deftest work-set-update-sensible
  (let [ss {1 {:id 1 :points 10 :link "sweet"}
            2 {:id 2 :points 100 :link "steve :("}
            3 {:id 3 :points 200 :link "love <3"}}
        ws (update-work-set {} ss)]
    (is (= ws (update-work-set ws ss)) "updating with same stories is idempotent")))

(deftest workset-update-keeps-newer
  (let [ws {1 {:id 1 :points 1}
            2 {:id 2 :points 2}}
        ns [{:id 2 :points 2000}
            {:id 3 :points 1}]
        ws2 (update-work-set ws ns)]
    (is (= #{1 2 3} (set (keys ws2))) "should include all stories")
    (is (= 2000 (get-in ws2 [2 :points])) "points for story 2 is from new list")))

