package com.example.bookcase;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.Executors;

@Database(entities = {Book.class}, version = 1)
public abstract class BookDatabase extends RoomDatabase {
    public abstract BookDao bookDao();

    private static volatile BookDatabase INSTANCE;

    public static BookDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BookDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BookDatabase.class, "book_database")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();

                    // Load books into the database
                    preloadBooks(INSTANCE);
                }
            }
        }
        return INSTANCE;
    }

    private static void preloadBooks(BookDatabase db) {
        Executors.newSingleThreadExecutor().execute(() -> {
            BookDao bookDao = db.bookDao();

            if (bookDao.getAllBooks().isEmpty()) { // Only insert if the database is empty
                Log.d("DEBUG", "Inserting books into database...");

                bookDao.insert(new Book("Interaction Design 2nd Edition", "Sharp, Rogers, and Preece", 1));
                bookDao.insert(new Book("Interaction Design 5th Edition", "Sharp, Rogers, and Preece", 1));
                bookDao.insert(new Book("Management and Cost Accounting", "Horngren, Bhimani, Foster, Datar", 1));
                bookDao.insert(new Book("Principles of Microeconomics", "Mankiw", 1));
                bookDao.insert(new Book("Macroeconomics", "Forlag", 1));
                bookDao.insert(new Book("Contemporary Strategy Analysis", "Grant", 1));
                bookDao.insert(new Book("Startup Funding", "Nielsen", 1));
                bookDao.insert(new Book("Statistical Methods for the Social Sciences", "Agresti, Finlay", 1));
                bookDao.insert(new Book("Economic Approaches to Organizations", "Douma, Schreuder", 1));
                bookDao.insert(new Book("Economics", "Sloman, Wride, Garratt", 1));
                bookDao.insert(new Book("Breaking Through to the Other Side", "Snitker", 1));
                bookDao.insert(new Book("Global Marketing", "Hollensen", 1));
                bookDao.insert(new Book("Principles and Practice of Marketing", "Jobber and Ellis-Chadwick", 1));
                bookDao.insert(new Book("Sales and Marketing", "Frandsen, Christiansen, Olsen, and Olesen", 1));
                bookDao.insert(new Book("Macroeconomics", "Blanchard", 1));
                bookDao.insert(new Book("Computer Networking", "Kurose, Ross", 1));
                bookDao.insert(new Book("Understanding the Theory and Design of Organizations", "Daft", 1));
                bookDao.insert(new Book("Visual Methodologies", "Rose", 1));
                bookDao.insert(new Book("The Group Project - How to Do It", "Petersen, Sorensen", 1));
                bookDao.insert(new Book("Web Design: Music Sites", "Wiedemann", 1));
                bookDao.insert(new Book("Innovation as Usual", "Miller, Wedell-Wedellsborg", 1));
                bookDao.insert(new Book("Social Research Methods", "Bryman", 1));
                bookDao.insert(new Book("An Introduction to Network Programming with Java", "Graba", 1));
                bookDao.insert(new Book("Introduction to Java Programming", "Liang", 1));
                bookDao.insert(new Book("Clinical Exercise Physiology", "Ehrman, Gordon, Visich, Keteyian", 1));
                bookDao.insert(new Book("Advanced Presentations by Design", "Abela", 1));
                bookDao.insert(new Book("Emery’s Elements of Medical Genetics", "Turnpenny, Ellard", 1));
                bookDao.insert(new Book("Mark’s Basic Medical Biochemistry", "Lieberman, D. Marks", 1));
                bookDao.insert(new Book("Service Design for Business", "Reason, Levlie, Flu", 1));

                bookDao.insert(new Book("Basics of Toxicology", "Kent", 2));
                bookDao.insert(new Book("Method and Project Writing", "Harboe", 2));
                bookDao.insert(new Book("Politics", "Heywood", 2));
                bookDao.insert(new Book("Handbook on European Data Protection Law", "FRA, EDPS, ECTHR", 2));
                bookDao.insert(new Book("Differential Equations and Infinite Series", "Christensen", 2));
                bookDao.insert(new Book("Learning Web Design", "Robbins", 2));
                bookDao.insert(new Book("Critical Thinking Skills", "Cottrell", 2));
                bookDao.insert(new Book("Book of Abstracts - Smart Energy Systems", "Unknown", 2));
                bookDao.insert(new Book("Psychiatric Nursing Conference Book - 2019", "Unknown", 2));
                bookDao.insert(new Book("Grammar in Practice", "Gower", 2));
                bookDao.insert(new Book("Progress to Proficiency", "Jones", 2));
                bookDao.insert(new Book("Longman Proficiency Practice Exams", "Kingsbury, Wellman", 2));
                bookDao.insert(new Book("Advanced Grammar in Use", "Hewings", 2));
                bookDao.insert(new Book("Business Model Generation", "Wiley", 2));

                bookDao.insert(new Book("Dianetica", "Hubbard", 3));
                bookDao.insert(new Book("Indrumar pt Restabilirea Sanatatii", "Popa", 3));
                bookDao.insert(new Book("Codul Bunelor Maniere Astazi", "Marinescu", 3));
                bookDao.insert(new Book("Secretele Succesului in Era Digitala", "Carnegie", 3));
                bookDao.insert(new Book("Cum sa Va Controlati Anxietatea", "Ellis", 3));
                bookDao.insert(new Book("Exercitii de Echilibru", "Chirila", 3));
                bookDao.insert(new Book("Tu Mai Vrei, El Nu Mai Vrea", "Behrendt", 3));
                bookDao.insert(new Book("Constelatii - Ghid de Buzunar", "Unknown", 3));
                bookDao.insert(new Book("Manual de Supravietuire", "Derlogea", 3));
                bookDao.insert(new Book("250 de Lucruri de Incercat Intr-o Viata", "Rijck", 3));
                bookDao.insert(new Book("Verbul", "Croitoru", 3));
                bookDao.insert(new Book("Gramatica Limbii Engleze", "Starceanu", 3));
                bookDao.insert(new Book("Codul lui Da Vinci", "Brown", 3));

                bookDao.insert(new Book("The Leadership Experience", "Daft", 4));
                bookDao.insert(new Book("Coaching for Performance", "Whitmore", 4));
                bookDao.insert(new Book("Leadership from the Inside", "Cashman", 4));
                bookDao.insert(new Book("Control of Communicable Diseases Manual", "Unknown", 4));
                bookDao.insert(new Book("The Recipe for Business Success", "Nelson", 4));
                bookDao.insert(new Book("The Power of Habit", "Duhigg", 4));
                bookDao.insert(new Book("How to Make Friends and Influence People", "Carnegie", 4));
                bookDao.insert(new Book("Beyond Order", "Peterson", 4));
                bookDao.insert(new Book("12 Rules for Life", "Peterson", 4));
                bookDao.insert(new Book("You Are Not So Smart", "McRaney", 4));
                bookDao.insert(new Book("Daily Reflections for Highly Effective People", "Covey", 4));

                bookDao.insert(new Book("Ingeri si Demoni", "Brown", 5));
                bookDao.insert(new Book("Inferno", "Brown", 5));
                bookDao.insert(new Book("Conspiratia", "Brown", 5));
                bookDao.insert(new Book("Digital Fortress", "Brown", 5));
                bookDao.insert(new Book("Origin", "Brown", 5));
                bookDao.insert(new Book("Manipularea Subalternilor", "Johnson Jr.", 5));
                bookDao.insert(new Book("Paradisurile Artificiale", "Baudelaire", 5));
                bookDao.insert(new Book("Oceanografie", "Eliade", 5));
                bookDao.insert(new Book("Tata Bogat, Tata Sarac", "Kiyosaki", 5));
                bookDao.insert(new Book("Jane Eyre", "Bronte", 5));
                bookDao.insert(new Book("Apa pentru Elefanti", "Gruen", 5));
                bookDao.insert(new Book("Ma Sinucid Alta Data", "Gier", 5));
                bookDao.insert(new Book("PS Te Iubesc", "Ahern", 5));
                bookDao.insert(new Book("Ce-as Fi Eu Fara Tine", "Musso", 5));
                bookDao.insert(new Book("Chemarea Ingerului", "Musso", 5));
                bookDao.insert(new Book("Ce sa Fac cu Mostenirea", "Gier", 5));
                bookDao.insert(new Book("Diavolul din Milano", "Suter", 5));
                bookDao.insert(new Book("Totul despre Sex", "Bushnell", 5));
                bookDao.insert(new Book("Valentine", "Trigiani", 5));
                bookDao.insert(new Book("Micuta Englezoaica", "Sanderson", 5));
                bookDao.insert(new Book("Greselile Mirandei", "Mansell", 5));
                bookDao.insert(new Book("Iubit de Imprumut", "Giffin", 5));
                bookDao.insert(new Book("Ultima Scrisoare de Dragoste", "Moyes", 5));
                bookDao.insert(new Book("Cred ca Te Iubesc", "Pearson", 5));
                bookDao.insert(new Book("Intrebari de-a Gata pt Interviu", "Peel", 5));
                bookDao.insert(new Book("Pentru o Fiica Speciala", "Exley", 5));
                bookDao.insert(new Book("Fiicei Mele cu Dragoste", "Exley", 5));
                bookDao.insert(new Book("Fata din Tren", "Hawkins", 5));
                bookDao.insert(new Book("Fetele Bune Ajung in Rai, Fetele Rele Unde Vor", "Ehrhardt", 5));
                bookDao.insert(new Book("Spovedania unui Ucigas", "Roth", 5));
                bookDao.insert(new Book("La Cumparaturi cu Minnie", "Kinsella", 5));
                bookDao.insert(new Book("Extrem de Tare si Incredibil de Aproape", "Foer", 5));
                bookDao.insert(new Book("Cainii din Riga", "Mankell", 5));
                bookDao.insert(new Book("Ghid de Conversatie Roman Maghiar", "Polirom", 5));
                bookDao.insert(new Book("Mica Enciclopedie Hygge", "Wiking", 5));
                bookDao.insert(new Book("Biblia Pierduta", "Bergler", 5));
                bookDao.insert(new Book("Don Quixote", "Cervantes", 5));
                bookDao.insert(new Book("Idiotul", "Dostoievski", 5));

                bookDao.insert(new Book("Romania and Moldova", "Unknown", 6));
                bookDao.insert(new Book("Honduras and the Bay Islands", "Unknown", 6));
                bookDao.insert(new Book("Morocco", "Unknown", 6));
                bookDao.insert(new Book("Barcelona", "Unknown", 6));
                bookDao.insert(new Book("Austria", "Unknown", 6));
                bookDao.insert(new Book("Egypt", "Unknown", 6));
                bookDao.insert(new Book("Greek Islands", "Unknown", 6));
                bookDao.insert(new Book("London", "Unknown", 6));
                bookDao.insert(new Book("Thailand", "Unknown", 6));
                bookDao.insert(new Book("Mallorca Boom", "Unknown", 6));
                bookDao.insert(new Book("Wonders of Iceland", "Unknown", 6));
                bookDao.insert(new Book("Harry Potter and the Sorcerer’s Stone", "J.K. Rowling", 6));
                bookDao.insert(new Book("Harry Potter and the Chamber of Secrets", "J.K. Rowling", 6));
                bookDao.insert(new Book("Harry Potter and the Prisoner of Azkaban", "J.K. Rowling", 6));
                bookDao.insert(new Book("Harry Potter and the Deathly Hallows", "J.K. Rowling", 6));

                bookDao.insert(new Book("Learn Danish with Starter Stories", "Hyplearn", 7));
                bookDao.insert(new Book("Danish for Children", "Andrea", 7));
                bookDao.insert(new Book("Shortcut to Danish", "Andersen", 7));
                bookDao.insert(new Book("Din Krop", "Unknown", 7));
                bookDao.insert(new Book("Roskilde Festival", "Unknown", 7));
                bookDao.insert(new Book("Stifinderen", "Wacher, Kjaergaard", 7));
                bookDao.insert(new Book("På Vej til Dansk", "Thorborg, Riis", 7));
                bookDao.insert(new Book("Under Overfladen", "Jeppesen, Maribo", 7));
                bookDao.insert(new Book("En Shopaholic i New York", "Kinsella", 7));
                bookDao.insert(new Book("Mit København", "Bjerregaard", 7));
                bookDao.insert(new Book("Heidi", "Spyri", 7));
                bookDao.insert(new Book("Robinson Crusoe", "Defoe", 7));
                bookDao.insert(new Book("Tak for Din Ansøgning", "Valentin", 7));
                bookDao.insert(new Book("Den Sidste Romantiker", "Hall", 7));
                bookDao.insert(new Book("Den Stjålne Vej", "Riebnitzsky", 7));
                bookDao.insert(new Book("Shopaholic & Søster", "Kinsella", 7));
                bookDao.insert(new Book("Du Er Nok", "Moorjani", 7));
                bookDao.insert(new Book("Robin Hood", "Møller", 7));
                bookDao.insert(new Book("Skyldneren", "Kristiansen", 7));
                bookDao.insert(new Book("Jumbobog Comic Books - 27 and 389", "Unknown", 7));
                bookDao.insert(new Book("Alexandre Le Grand", "Cloche", 7));
                bookDao.insert(new Book("Forestilling om Kærlighed", "Wesley", 7));
                bookDao.insert(new Book("Verdens Vidundere", "Unknown", 7));
                bookDao.insert(new Book("Pretty Little Liars", "Shepard", 7));
                bookDao.insert(new Book("Københavnerne", "Stensgaard, Schaldemose", 7));
                bookDao.insert(new Book("En Verden af Pasta", "Jaros, Beer", 7));
                bookDao.insert(new Book("Det er Mere Bar’ Mad", "Oliver", 7));

                bookDao.insert(new Book("Det er Bar’ Mad", "Oliver", 8));
                bookDao.insert(new Book("Spar med Jamie", "Oliver", 8));
                bookDao.insert(new Book("Den Danske Salme Bog", "Unknown", 8));
                bookDao.insert(new Book("The Mind of the Leader", "Hougaard, Dybkjær, Larsen", 8));
                bookDao.insert(new Book("Salatbaren", "Lewis", 8));
                bookDao.insert(new Book("Photoshop på Dansk 2", "Unknown", 8));
                bookDao.insert(new Book("Dansk Basisgrammatik", "Hilt, Jensen, Rifbjerg", 8));
                bookDao.insert(new Book("Christiania", "Unknown", 8));
                bookDao.insert(new Book("Den Evige Ild", "Follett", 8));
                bookDao.insert(new Book("Desperate Housewives Kogebogen", "Unknown", 8));
                bookDao.insert(new Book("Bagebog", "Unknown", 8));
                bookDao.insert(new Book("Fame School", "Jefferies", 8));
                bookDao.insert(new Book("Strafferetlige Sanktioner", "Vestergaard", 8));
                bookDao.insert(new Book("Strafferettens Almindelige Del", "Waaben", 8));
                bookDao.insert(new Book("Dansk - Engelsk", "Unknown", 8));
                bookDao.insert(new Book("Engelsk - Dansk Dictionary", "Unknown", 8));
                bookDao.insert(new Book("Hot Sex Sådan", "Cox", 8));
                bookDao.insert(new Book("Ølbogen", "Nielsen", 8));
                bookDao.insert(new Book("Sund og Slank med Hay Diæten", "Habgood", 8));
                bookDao.insert(new Book("Underdynen", "Keyes", 8));


            }
        });
    }
}
