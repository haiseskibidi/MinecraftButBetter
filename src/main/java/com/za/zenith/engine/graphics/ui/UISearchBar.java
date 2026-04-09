package com.za.zenith.engine.graphics.ui;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.utils.I18n;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Переиспользуемый компонент поисковой строки.
 * Поддерживает расширенное управление: выделение, копирование, вставку и перемещение курсора.
 */
public class UISearchBar {
    private String query = "";
    private boolean focused = false;
    private final String placeholder;
    
    private int cursorPos = 0;
    private int selectionStart = -1; // -1 означает отсутствие выделения
    
    private int x, y, width, height;

    public UISearchBar(String placeholderKey) {
        this.placeholder = I18n.get(placeholderKey);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(UIRenderer renderer, int sw, int sh) {
        // 1. Фон строки
        renderer.getPrimitivesRenderer().renderRect(x, y, width, height, sw, sh, 0.1f, 0.1f, 0.1f, 1.0f);
        
        // 2. Подсветка фокуса
        if (focused) {
            renderer.getPrimitivesRenderer().renderRect(x, y, width, height, sw, sh, 0.0f, 0.6f, 1.0f, 0.15f);
        }

        // 3. Выделение текста (если есть)
        if (focused && selectionStart != -1 && selectionStart != cursorPos) {
            renderSelection(renderer, sw, sh);
        }

        // 4. Текст или плейсхолдер
        if (query.isEmpty() && !focused) {
            renderer.getFontRenderer().drawString(placeholder, x + 5, y + 5, 14, sw, sh, 0.5f, 0.5f, 0.5f, 1.0f);
        } else {
            renderer.getFontRenderer().drawString(query, x + 5, y + 5, 14, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // 5. Курсор
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = x + 5 + renderer.getFontRenderer().getStringWidth(query.substring(0, cursorPos), 14);
                renderer.getPrimitivesRenderer().renderRect(cursorX, y + 4, 1, height - 8, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private void renderSelection(UIRenderer renderer, int sw, int sh) {
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        
        int startX = x + 5 + renderer.getFontRenderer().getStringWidth(query.substring(0, start), 14);
        int selectionWidth = renderer.getFontRenderer().getStringWidth(query.substring(start, end), 14);
        
        renderer.getPrimitivesRenderer().renderRect(startX, y + 4, selectionWidth, height - 8, sw, sh, 0.0f, 0.4f, 0.8f, 0.5f);
    }

    public boolean handleMouseClick(float mx, float my, int button) {
        if (mx >= x && mx <= x + width && my >= y && my <= y + height) {
            focused = true;
            // При клике сбрасываем выделение и ставим курсор в конец (для простоты пока так)
            cursorPos = query.length();
            selectionStart = -1;
            return true;
        }
        focused = false;
        selectionStart = -1;
        return false;
    }

    public boolean handleKeyPress(int key) {
        if (!focused) return false;

        long window = GameLoop.getInstance().getWindow().getWindowHandle();
        boolean ctrl = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS;
        boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;

        // --- Управление курсором ---
        if (key == GLFW_KEY_LEFT) {
            if (shift && selectionStart == -1) selectionStart = cursorPos;
            else if (!shift) selectionStart = -1;
            
            if (cursorPos > 0) cursorPos--;
            return true;
        }
        if (key == GLFW_KEY_RIGHT) {
            if (shift && selectionStart == -1) selectionStart = cursorPos;
            else if (!shift) selectionStart = -1;
            
            if (cursorPos < query.length()) cursorPos++;
            return true;
        }
        if (key == GLFW_KEY_HOME) {
            if (shift && selectionStart == -1) selectionStart = cursorPos;
            else if (!shift) selectionStart = -1;
            cursorPos = 0;
            return true;
        }
        if (key == GLFW_KEY_END) {
            if (shift && selectionStart == -1) selectionStart = cursorPos;
            else if (!shift) selectionStart = -1;
            cursorPos = query.length();
            return true;
        }

        // --- Быстрые действия (Ctrl + ...) ---
        if (ctrl) {
            if (key == GLFW_KEY_A) { // Выделить всё
                selectionStart = 0;
                cursorPos = query.length();
                return true;
            }
            if (key == GLFW_KEY_C && selectionStart != -1) { // Копировать
                int start = Math.min(selectionStart, cursorPos);
                int end = Math.max(selectionStart, cursorPos);
                glfwSetClipboardString(window, query.substring(start, end));
                return true;
            }
            if (key == GLFW_KEY_V) { // Вставить
                String clipboard = glfwGetClipboardString(window);
                if (clipboard != null) {
                    deleteSelection();
                    insertText(clipboard);
                }
                return true;
            }
            if (key == GLFW_KEY_X && selectionStart != -1) { // Вырезать
                int start = Math.min(selectionStart, cursorPos);
                int end = Math.max(selectionStart, cursorPos);
                glfwSetClipboardString(window, query.substring(start, end));
                deleteSelection();
                return true;
            }
        }

        // --- Удаление ---
        if (key == GLFW_KEY_BACKSPACE) {
            if (selectionStart != -1 && selectionStart != cursorPos) {
                deleteSelection();
            } else if (cursorPos > 0) {
                query = query.substring(0, cursorPos - 1) + query.substring(cursorPos);
                cursorPos--;
            }
            return true;
        }
        if (key == GLFW_KEY_DELETE) {
            if (selectionStart != -1 && selectionStart != cursorPos) {
                deleteSelection();
            } else if (cursorPos < query.length()) {
                query = query.substring(0, cursorPos) + query.substring(cursorPos + 1);
            }
            return true;
        }

        // --- Завершение ввода ---
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_ESCAPE) {
            focused = false;
            selectionStart = -1;
            return true;
        }

        return true; 
    }

    public boolean handleChar(int codepoint) {
        if (focused) {
            deleteSelection();
            insertText(String.valueOf((char) codepoint));
            return true;
        }
        return false;
    }

    private void deleteSelection() {
        if (selectionStart == -1 || selectionStart == cursorPos) return;
        int start = Math.min(selectionStart, cursorPos);
        int end = Math.max(selectionStart, cursorPos);
        query = query.substring(0, start) + query.substring(end);
        cursorPos = start;
        selectionStart = -1;
    }

    private void insertText(String text) {
        query = query.substring(0, cursorPos) + text + query.substring(cursorPos);
        cursorPos += text.length();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        this.cursorPos = query.length();
        this.selectionStart = -1;
    }

    public boolean isFocused() {
        return focused;
    }
}
