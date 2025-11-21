# à garder pour scraper la liste des profs et leurs attributs sur 2 sites :

# www.ecam.be/annuaire ---> liste filtré )par id (diminutif de 3 caractères) , donc permet aussi d'avoir l'adresse
# Le seul souci c'est que le site est typé dynamiquement Wordpress --> Besoin du nom précis de la classe qui renferme
# la liste du personnel pour pouvoir récup la liste qui se met à jour chaque année à priori ---> évite de mettre à jour
# main à chaque fois.

# www.plus.ecam.be/public/bloc/2025 ----> Récup la liste des cours que chaque prof donne et donner le lien
# ---> parcourir chaque fiche
# d'UE et vérifier que le nom du prof est bien mentionné ---> Souci : nom des profs peut etre en majuscule ou minuscule
