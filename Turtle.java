import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane;
import java.awt.geom.*;
public class Turtle extends JFrame {
    private int x = 200;  // początkowe położenie X
    private int y = 200;  // początkowe położenie Y
    private int height = 1000;
    private int width = 1000;
    private List<Obstacle> obstacles = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<CollectibleObject> collectibleObjects = new ArrayList<>();
    private MovingObject movingObject;
    private double lives = 3;  // Początkowa liczba żyć
    private int score = 0;
    private int bulletsFired = 0;
    private boolean gameRunning = true;
  
    private LoadingIndicator loadingIndicator;

    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;

    private Timer timer;
    private Timer collectibleTimer;
    private Timer objectTimer;
    private Timer bulletTimer;

    private long lastUpdateTime;
    private double deltaTime;
    
    public Turtle() {
        setTitle("Żółwik");
        setSize(height, width);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        lastUpdateTime = System.nanoTime();

        // Dodanie obiektu klasy TurtlePanel do ramki
        TurtlePanel turtlePanel = new TurtlePanel();
        add(turtlePanel);

        // Dodanie KeyListener do obsługi strzałek
        addKeyListener(new TurtleKeyListener());

        // Dodanie MouseListener do obsługi myszy
        addMouseListener(new TurtleMouseListener());

        // Ustawienie focusable, aby okno mogło odbierać zdarzenia od klawiatury
        setFocusable(true);

        loadingIndicator = new LoadingIndicator(width - 100, height - 30, 100, 20, 5000);  // Przykładowe parametry

        // Timer do cyklicznego dodawania nowych przeszkód 
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addRandomObstacle();
            }
        });
        timer.start();
        
        // Timer do cyklicznego dodawania nowych obiektów do zebrania
        collectibleTimer = new Timer(5000, new ActionListener() { // np. co 5 sekund
            @Override
            public void actionPerformed(ActionEvent e) {
            	addCollectible();
            }
        });
        collectibleTimer.start();
        
        // Timer do aktualizacji pozycji obiektu poruszającego się z prawej do lewej
        objectTimer = new Timer(20, new ActionListener() {
            private long lastTime = System.nanoTime();

            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.nanoTime();
                double deltaTime = (now - lastTime) / 1e9; // Czas między klatkami w sekundach
                lastTime = now;
                loadingIndicator.updateCooldown(deltaTime);
                moveCollectibles(deltaTime);
                moveObstacles(deltaTime); // Aktualizacja pozycji przeszkód
                moveBullets(deltaTime);   // Aktualizacja pozycji pocisków
                checkCollision(); // sprawdzanie kolizji
                repaint();
            }
        });
        objectTimer.start();

        // Timer do stałego wystrzeliwania małych pocisków
        bulletTimer = new Timer(1, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (leftMousePressed && loadingIndicator.loaded) {
                    fireSmallBullet();
                    loadingIndicator.resetCooldown();  // Zresetuj wskaźnik ładowania
                }
            }
        });
        bulletTimer.start();
    }

    // Sprawdzanie kolizji z przeszkodami
    private void checkCollision() {        
        // Sprawdzanie kolizji pocisku z przeszkodami
        for (Iterator<Bullet> bulletIterator = bullets.iterator(); bulletIterator.hasNext(); ) {
            Bullet bullet = bulletIterator.next();
            Rectangle bulletBounds = bullet.getBounds();
            
            for (Iterator<Obstacle> obstacleIterator = obstacles.iterator(); obstacleIterator.hasNext(); ) {
                Obstacle obstacle = obstacleIterator.next();
                if (!obstacle.isDestroyed() && bulletBounds.intersects(obstacle.getBounds())) {
                    shootObstacle(bullet,obstacle);
                    bulletIterator.remove();
                }
            }
        }
        //kolizja żółwika z przeszkodą
        Rectangle turtleBounds = new Rectangle(x, y, 50, 50);
        for (Obstacle obstacle : obstacles) {
            if (turtleBounds.intersects(obstacle.getBounds())) {
                // Kolizja z przeszkodą, można dodać odpowiednią obsługę
                handleCollisionWithObstacle(obstacle);
            }
        }
        
     // Sprawdzanie kolizji z obiektem do zebrania
        for (CollectibleObject collectible : collectibleObjects) {
            Area turtleArea = new Area(turtleBounds);
            Area collectibleArea = new Area(collectible.getBounds());

            turtleArea.intersect(collectibleArea);

            if (!turtleArea.isEmpty()) {
                handleCollisionWithCollectible(collectible);
                // Tutaj możesz dodać dodatkowe operacje, takie jak usunięcie zebranego obiektu
                collectibleObjects.remove(collectible);
                repaint();
                break;  // Jeśli chcesz obsługiwać tylko jedno zebranie na raz, dodaj break
            }
        }

    }
    
    private void handleCollisionWithCollectible(CollectibleObject collectible) {
        // Obsługa zebrania obiektu do zebrania
        score += 5; // Przykładowe dodatkowe punkty
        collectible.collect();
        System.out.println("Collectible object collected! Score: " + score);
    }
    
    private void handleCollisionWithMovingObject() {
        // Obsługa kolizji z obiektem poruszającym się z prawej do lewej
        lives -= 1.0;
        System.out.println("Collision with moving object! Lives: " + lives);
        
        if (lives <= 0) {
            // Gracz stracił wszystkie życia, zatrzymaj grę i wyświetl komunikat "Game Over"
            System.out.println("Game over!");
            gameRunning = false;
            timer.stop();
            collectibleTimer.stop();
            objectTimer.stop();
            bulletTimer.stop();
        }
    }
   
    private void handleCollisionWithObstacle(Obstacle obstacle) {
        if (!obstacle.isDestroyed()) {
            obstacle.destroy();
            System.out.println("Obstacle destroyed by turtle!");

            double damage = 0.0;

            if (obstacle instanceof SmallObstacle) {
                damage = 0.25;
            } else if (obstacle instanceof MediumObstacle) {
                damage = 0.5;
            } else if (obstacle instanceof LargeObstacle) {
                damage = 1.0;
            }
            
            lives -= damage;

            if (lives <= 0) {
                // Gracz stracił wszystkie życia, zatrzymaj grę i wyświetl komunikat "Game Over"
                System.out.println("Game over!");
            
            }

            System.out.println("Lives: " + lives);
        }
    }
 
    private void shootObstacle(Bullet bullet, Obstacle obstacle) {
        if (!obstacle.isDestroyed()) {

            double damage = 0.0;
            damage = bullet.damage;
            obstacle.health -= damage;
            System.out.println("Dane: health=" + obstacle.getHealth() + ", damage=" + bullet.getDamage());
            if (obstacle.health <= 0) {
                // Przeszkoda została zniszczona więc ją usuwamy
                System.out.println("Obstacle destroyed!");
                score += 1; 
                obstacle.destroy();
            }
        }
    }
    // Dodanie losowej przeszkody
    private void addRandomObstacle() {
        Random random = new Random();
        int obstacleX = width; // Początkowe położenie przeszkody na prawym krańcu
        int obstacleY = random.nextInt(height - 50);

        // Losowy wybór typu przeszkody
        int obstacleType = random.nextInt(3); // Załóżmy, że mamy trzy typy przeszkód

        Obstacle obstacle;
        switch (obstacleType) {
            case 0:
                obstacle = new SmallObstacle(obstacleX, obstacleY);
                break;
            case 1:
                obstacle = new MediumObstacle(obstacleX, obstacleY);
                break;
            case 2:
                obstacle = new LargeObstacle(obstacleX, obstacleY);
                break;
            default:
                obstacle = new SmallObstacle(obstacleX, obstacleY);
                break;
        }

        obstacles.add(obstacle);
        repaint();
    }

    //  wystrzeliwanie małych pocisków
    private void fireSmallBullet() {
        Bullet bullet = new SmallBullet(x + 50, y + 25);
        bullets.add(bullet);

        // Zaktualizuj licznik pocisków
        bulletsFired += 1;
        System.out.println("Bullets Fired: " + bulletsFired);

        repaint();
    }

    private void addCollectible() {
        Random random = new Random();
        int objectX = width; // Początkowe położenie na prawym krańcu
        int objectY = random.nextInt(height - 50);

        // Tworzenie obiektu do zebrania
        CollectibleObject object = new CollectibleObject(objectX, objectY, 50, 50);
        collectibleObjects.add(object);
        repaint();
    }

    // Wystrzeliwanie dużego pocisku
    private void fireLargeBullet() {
        Bullet bullet = new LargeBullet(x + 50, y + 25);
        bullets.add(bullet);

        // Zaktualizuj licznik pocisków
        bulletsFired += 1;
        System.out.println("Bullets Fired: " + bulletsFired);

        repaint();
    }

    // Aktualizacja pozycji przeszkód
    private void moveObstacles(double deltaTime) {
        for (Obstacle obstacle : obstacles) {
            obstacle.moveLeft(deltaTime);
        }
    }
  
    private void moveCollectibles(double deltaTime) {
        for (CollectibleObject collectible : collectibleObjects) {
        	collectible.moveLeft(deltaTime);
        }
    }

    // Aktualizacja pozycji pocisków
    private void moveBullets(double deltaTime) {
        for (Iterator<Bullet> iterator = bullets.iterator(); iterator.hasNext(); ) {
            Bullet bullet = iterator.next();
            bullet.moveRight(deltaTime);

            // Usuwanie pocisków, które wyszły poza ekran
            if (bullet.getX() > width) {
                iterator.remove();
            }
        }
    }

    // Stworzenie żółwika
    private class TurtlePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Oblicz deltaTime
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastUpdateTime) / 1e9; // Konwersja na sekundy
            lastUpdateTime = currentTime;

            // Rysowanie przeszkód
            for (Obstacle obstacle : obstacles) {
                if (!obstacle.isDestroyed()) {
                    obstacle.draw(g);
                }
            }

            // Rysowanie żółwika
            g.setColor(Color.GREEN);
            g.fillRect(x, y, 50, 50);

            // Rysowanie pocisków
            for (Bullet bullet : bullets) {
                if (!bullet.isDestroyed()) {
                    bullet.draw(g);
                }
            }

            // Rysowanie obiektów do zebrania
            for (CollectibleObject collectible : collectibleObjects) {
                if (collectible.isCollected()) {
                    collectible.draw(g);
                }
            }
            
            // Rysowanie wskaźnika ładowania
            loadingIndicator.draw(g, 100);

            // Rysowanie ilości żyć
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Lives: " + lives, 10, 20);

            // Rysowanie punktacji
            g.setColor(Color.BLUE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Score: " + score, getWidth() - 100, 20);

            // Rysowanie wystrzelonych pocisków
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Bullets Fired: " + bulletsFired, getWidth() - 200, 40);

            // Rysowanie "Game Over" i punktacji, jeśli gra zakończona
            if (!gameRunning || lives <= 0) {
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 50));
                String gameOverText = "Game Over";
                g.drawString(gameOverText, (getWidth() - g.getFontMetrics().stringWidth(gameOverText)) / 2, getHeight() / 2 - 25);

                g.setColor(Color.BLUE);
                g.setFont(new Font("Arial", Font.PLAIN, 30));
                String scoreText = "Score: " + score;
                g.drawString(scoreText, (getWidth() - g.getFontMetrics().stringWidth(scoreText)) / 2, getHeight() / 2 + 25);

                timer.stop();
                collectibleTimer.stop();
                objectTimer.stop();
                bulletTimer.stop();
            }
        }
    }

    // Ruch żółwikiem
    private class TurtleKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
            // Ignorujemy zdarzenie keyTyped
        }

        @Override
        public void keyPressed(KeyEvent e) {
            // Obsługa naciśnięcia klawisza
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_UP:
                    y -= 10;  // przesunięcie w górę
                    break;
                case KeyEvent.VK_DOWN:
                    y += 10;  // przesunięcie w dół
                    break;
                case KeyEvent.VK_LEFT:
                    x -= 10;  // przesunięcie w lewo
                    break;
                case KeyEvent.VK_RIGHT:
                    x += 10;  // przesunięcie w prawo
                    break;
            }

            // Odświeżenie panelu
            repaint();
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Ignorujemy zdarzenie keyReleased
        }
    }

    // Obsługa myszy, strzelanie pociskami
    private class TurtleMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                // Lewy przycisk myszy - wystrzeliwanie małego pocisku
                leftMousePressed = true;               
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                // Prawy przycisk myszy - wystrzeliwanie dużego pocisku
                fireLargeBullet();
                rightMousePressed = true;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                // Releasing the left mouse button
                leftMousePressed = false;
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                // Releasing the right mouse button
                rightMousePressed = false;
            }
        }
    }
 // Dodaj nową klasę do reprezentowania wskaźnika ładowania
    private class LoadingIndicator {
        private int x;
        private int y;
        private int width;
        private int height;
        private int maxCooldown;  // Maksymalny czas ładowania w milisekundach
        private int currentCooldown;  // Aktualny czas ładowania
        private boolean loaded = false; 
        public LoadingIndicator(int x, int y, int width, int height, int maxCooldown) {
            this.x = 10;
            this.y = 25;
            this.width = width;
            this.height = height;
            this.maxCooldown = maxCooldown;
            this.currentCooldown = 0;
        }

        public void resetCooldown() {
            currentCooldown = 0;
            loaded = false; 
        }

        public void updateCooldown(double deltaTime) {
            currentCooldown += deltaTime * 1000;  // Przemnóż przez 1000, aby zamienić sekundy na milisekundy
        }

        public void draw(Graphics g, double deltaTime) {
        	   // Rysuj prostokąt wskaźnika ładowania
            g.setColor(Color.GREEN);
            int indicatorWidth = (int) (width * (1 - currentCooldown / (double) maxCooldown));
            if(indicatorWidth == 0) loaded = true;
            if(!loaded)
            	g.fillRect(x, y, indicatorWidth , height);
        }
    
        public boolean isCooldownActive() {
            return currentCooldown > 0 && currentCooldown < maxCooldown;
        }
    }
   
    // Nie używane Klasa reprezentująca obiekt poruszający się z prawej do lewej
    private class MovingObject {
        private int x;
        private int y;
        private int width;
        private int height;
        private int speed;

        public MovingObject(int initialX, int initialHeight) {
            x = width = initialX;
            y = (height - initialHeight) / 2;
            height = initialHeight;
            speed = 5;
        }

        public void moveLeft(double deltaTime) {
            x -= speed * deltaTime * 60; // Przesunięcie z uwzględnieniem czasu między klatkami
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

 // Klasa bazowa reprezentująca przeszkodę
    private abstract class Obstacle {
        protected int x;
        protected int y;
        protected int health;  // określa wytrzymałość przeszkody
        protected boolean destroyed = false;

        public Obstacle(int x, int y, int health) {
            this.x = x;
            this.y = y;
            this.health = health;
        }

        public abstract void draw(Graphics g);

        public Rectangle getBounds() {
            return new Rectangle(x, y, getWidth(), getHeight());
        }

        public abstract void moveLeft(double deltaTime);

        public abstract int getWidth();

        public abstract int getHeight();

        public boolean isDestroyed() {
            return destroyed;
        }

        public void destroy() {
            destroyed = true;
        }

        public int getHealth() {
            return health;
        }

        public void decreaseHealth(int amount) {
            health -= amount;
            if (health <= 0) {
                destroy();
            }
        }
    }

    // Przeszkoda małego rozmiaru
    private class SmallObstacle extends Obstacle {
        public SmallObstacle(int x, int y) {
            super(x, y, 1);  // Mała przeszkoda ma 1 hp
        }

        @Override
        public void draw(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, 20, 20);
        }

        @Override
        public void moveLeft(double deltaTime) {
            x -= 2 * deltaTime * 60;
        }

        @Override
        public int getWidth() {
            return 20;
        }

        @Override
        public int getHeight() {
            return 20;
        }
    }

    // Przeszkoda średniego rozmiaru
    private class MediumObstacle extends Obstacle {
        public MediumObstacle(int x, int y) {
            super(x, y, 2);  // Średnia przeszkoda ma 2 hp
        }

        @Override
        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, 30, 30);
        }

        @Override
        public void moveLeft(double deltaTime) {
            x -= 1 * deltaTime * 60;
        }

        @Override
        public int getWidth() {
            return 30;
        }

        @Override
        public int getHeight() {
            return 30;
        }
    }

    // Przeszkoda dużego rozmiaru
    private class LargeObstacle extends Obstacle {
        public LargeObstacle(int x, int y) {
            super(x, y, 5);  // Duża przeszkoda ma 2 hp
        }

        @Override
        public void draw(Graphics g) {
            g.setColor(Color.GRAY);
            g.fillRect(x, y, 40, 40);
        }

        @Override
        public void moveLeft(double deltaTime) {
            x -= 1 * deltaTime * 60;
        }

        @Override
        public int getWidth() {
            return 30;
        }

        @Override
        public int getHeight() {
            return 30;
        }
    }
    
    // Klasa bazowa reprezentująca pocisk
    private abstract class Bullet {
        protected int x;
        protected int y;
        protected int speed;
        protected int damage;
        protected boolean destroyed = false;

        public Bullet(int x, int y, int d) {
            this.x = x;
            this.y = y;
            this.speed = 10;
            this.damage = d; 
        }

        public abstract void moveRight(double deltaTime);

        public abstract void draw(Graphics g);

        public Rectangle getBounds() {
            return new Rectangle(x, y, getWidth(), getHeight());
        }

        public abstract int getWidth();

        public abstract int getHeight();
        
        public abstract int getX();

        public abstract int getDamage();

        public void destroy() {
            destroyed = true;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

    }

    // Mały pocisk
    private class SmallBullet extends Bullet {
        public SmallBullet(int x, int y) {
            super(x, y,5);
        }

        @Override
        public void moveRight(double deltaTime) {
            x += speed * deltaTime * 60; // Przesunięcie z uwzględnieniem czasu między klatkami
        }

        @Override
        public void draw(Graphics g) {
            g.setColor(Color.MAGENTA);
            g.fillRect(x, y, 10, 10);
        }

        @Override
        public int getWidth() {
            return 10;
        }

        @Override
        public int getHeight() {
            return 5;
        }

        @Override
        public int getX() {
            return x;
        }
        
        @Override
        public int getDamage() {
            return damage;
        }
    }

    // Duży pocisk
    private class LargeBullet extends Bullet {
        public LargeBullet(int x, int y) {
            super(x, y, 1);
            this.speed = 5;
        }

        @Override
        public void moveRight(double deltaTime) {
            x += speed * deltaTime * 60; // Przesunięcie z uwzględnieniem czasu między klatkami
        }

        @Override
        public void draw(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, 20, 10);
        }

        @Override
        public int getWidth() {
            return 20;
        }

        @Override
        public int getHeight() {
            return 10;
        }
        
        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getDamage() {
            return damage;
        }
    }

    // Klasa reprezentująca obiekt do zebrania
    private class CollectibleObject {
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean exist = true; 
        public CollectibleObject(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public void moveLeft(double deltaTime) {
            x -= 1 * deltaTime * 60;
        }
        public void draw(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillOval(x, y, width, width);
        }

        public Ellipse2D.Double getBounds() {
            return new  Ellipse2D.Double(x, y, width, height);
        }
        public boolean isCollected() {
            return exist;
        }
        public void collect() {
            exist = false;
        }

    }
 
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Turtle turtle = new Turtle();
            turtle.setVisible(true);
        });
    }
}