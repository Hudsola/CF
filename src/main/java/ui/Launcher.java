package ui;

/**
 * Ponto de entrada do JAR executável.
 *
 * Quando a classe principal estende {@code Application} e o JavaFX não está no module path,
 * o Java aborta com "JavaFX runtime components are missing". Chamar o App a partir de uma
 * classe comum evita essa checagem e permite rodar com {@code java -jar}.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
