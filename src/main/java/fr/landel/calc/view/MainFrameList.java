package fr.landel.calc.view;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.border.Border;

import fr.landel.calc.config.Conf;
import fr.landel.calc.config.Formula;

public class MainFrameList {

    private static final Color BACKGROUND_FORMULA = new Color(230, 230, 255);
    private static final Color BACKGROUND_SELECTED_FORMULA = new Color(205, 205, 255);
    private static final Color BORDER_FORMULA = new Color(230, 230, 255);
    private static final Color BORDER_SELECTED_FORMULA = new Color(0, 0, 150);

    private static final Color BACKGROUND_SUCCESS = new Color(230, 255, 230);
    private static final Color BACKGROUND_SELECTED_SUCCESS = new Color(175, 225, 175);
    private static final Color BORDER_SUCCESS = new Color(230, 255, 230);
    private static final Color BORDER_SELECTED_SUCCESS = new Color(0, 150, 0);

    private static final Color BACKGROUND_ERROR = new Color(255, 230, 230);
    private static final Color BACKGROUND_SELECTED_ERROR = new Color(255, 200, 200);
    private static final Color BORDER_ERROR = new Color(255, 230, 230);
    private static final Color BORDER_SELECTED_ERROR = new Color(150, 0, 0);

    private static Color aCellColor[][];
    private static Border aBorder[][];
    private static int aAlignment[];

    private final JList<String> screenList;
    private final List<Formula> formulas;
    private final List<CounterListener> counterListeners;

    public MainFrameList(final JList<String> screenList, final List<Formula> formulas) {
        this.screenList = screenList;
        this.formulas = formulas;
        this.counterListeners = new ArrayList<>();
    }

    public void addCounterListener(final CounterListener listener) {
        this.counterListeners.add(listener);
    }

    public void addFormula(final Formula formula) {
        this.addFormula(formula.getFormula());
        formula.getResult().ifPresent(result -> {
            if (result.isSuccess()) {
                this.addResultSuccess(result.getResult());
            } else {
                this.addResultError(result.getResult());
            }
        });
        fireCounter();
    }

    public void addFormula(final String formula, final String result, final boolean success) {
        this.formulas.add(new Formula(formula, success, result));
        this.addFormula(formula);
        if (success) {
            this.addResultSuccess(result);
        } else {
            this.addResultError(result);
        }
        screenList.ensureIndexIsVisible(screenList.getModel().getSize() - 1);
        fireCounter();
    }

    private void addFormula(final String text) {
        add(screenList, -1, text);
        setColor(screenList, -1, BACKGROUND_FORMULA, Color.BLACK, BACKGROUND_SELECTED_FORMULA, Color.BLACK);
        setBorderColor(screenList, -1, BORDER_FORMULA, BORDER_SELECTED_FORMULA);
        setAlignment(screenList, -1, 2);

    }

    private void addResultSuccess(final String text) {
        add(screenList, -1, text);
        setColor(screenList, -1, BACKGROUND_SUCCESS, Color.BLACK, BACKGROUND_SELECTED_SUCCESS, Color.BLACK);
        setBorderColor(screenList, -1, BORDER_SUCCESS, BORDER_SELECTED_SUCCESS);
        setAlignment(screenList, -1, 4);
    }

    private void addResultError(final String text) {
        add(screenList, -1, text);
        setColor(screenList, -1, BACKGROUND_ERROR, Color.BLACK, BACKGROUND_SELECTED_ERROR, Color.BLACK);
        setBorderColor(screenList, -1, BORDER_ERROR, BORDER_SELECTED_ERROR);
        setAlignment(screenList, -1, 4);
    }

    private static void add(final JList<String> list, final int index, final String text) {
        int j = 0;
        String content = text;
        if (text.isBlank()) {
            content = " ";
        } else {
            content = text;
        }
        ListModel<String> model = list.getModel();
        int len = model.getSize();
        if (index >= -1 && index < len) {
            String aList[] = new String[len + 1];
            Color aCellColorTmp[][] = new Color[len + 1][4];
            Border aBorderTmp[][] = new Border[len + 1][2];
            int aAlignmentTmp[] = new int[len + 1];
            for (int i = 0; i < len; i++) {
                if (index == i) {
                    aList[i] = content;
                    aCellColorTmp[i][0] = Color.WHITE;
                    aCellColorTmp[i][1] = Color.BLACK;
                    aCellColorTmp[i][2] = new Color(184, 207, 229);
                    aCellColorTmp[i][3] = Color.BLACK;
                    aBorderTmp[i][0] = BorderFactory.createLineBorder(Color.WHITE);
                    aBorderTmp[i][1] = BorderFactory.createLineBorder(new Color(99, 130, 191));
                    aAlignmentTmp[i] = 2;
                    j++;
                }
                aList[i + j] = model.getElementAt(i).toString();
                aCellColorTmp[i + j][0] = aCellColor[i][0];
                aCellColorTmp[i + j][1] = aCellColor[i][1];
                aCellColorTmp[i + j][2] = aCellColor[i][2];
                aCellColorTmp[i + j][3] = aCellColor[i][3];
                aBorderTmp[i + j][0] = aBorder[i][0];
                aBorderTmp[i + j][1] = aBorder[i][1];
                aAlignmentTmp[i + j] = aAlignment[i];
            }

            if (index == -1) {
                aList[len] = content;
                aCellColorTmp[len][0] = Color.WHITE;
                aCellColorTmp[len][1] = Color.BLACK;
                aCellColorTmp[len][2] = new Color(184, 207, 229);
                aCellColorTmp[len][3] = Color.BLACK;
                aBorderTmp[len][0] = BorderFactory.createLineBorder(Color.WHITE);
                aBorderTmp[len][1] = BorderFactory.createLineBorder(new Color(99, 130, 191));
                aAlignmentTmp[len] = 2;
            }
            list.setListData(aList);
            list.setCellRenderer(new MyCellRenderer<>(list, aCellColorTmp, aBorderTmp, aAlignmentTmp));
            getRenderer(list);
        }
    }

    public void clear() {
        screenList.setListData(new String[0]);
        this.formulas.clear();
        fireCounter();
    }

    public void removeSelected() {
        Arrays.stream(screenList.getSelectedIndices()).boxed().sorted(Collections.reverseOrder()).forEach(i -> {
            final int index = i.intValue();
            int formula;
            int result;
            if (index % 2 == 0) {
                formula = index;
                result = index + 1;
            } else {
                formula = index - 1;
                result = index;
            }
            this.remove(result);
            this.remove(formula);

            final int subIndex = formula / 2;
            if (this.formulas.size() > subIndex) {
                this.formulas.remove(subIndex);
                Conf.setFormula(subIndex, null);
            }
        });
        fireCounter();
    }

    private void remove(final int index) {
        int j = 0;
        final ListModel<String> model = screenList.getModel();
        final int len = model.getSize();
        if (index >= 0 && index < len) {
            String aList[] = new String[len - 1];
            Color aCellColorTmp[][] = new Color[len - 1][4];
            Border aBorderTmp[][] = new Border[len - 1][2];
            int aAlignmentTmp[] = new int[len - 1];
            for (int i = 0; i < len; i++)
                if (index != i) {
                    aList[i + j] = model.getElementAt(i).toString();
                    aCellColorTmp[i + j][0] = aCellColor[i][0];
                    aCellColorTmp[i + j][1] = aCellColor[i][1];
                    aCellColorTmp[i + j][2] = aCellColor[i][2];
                    aCellColorTmp[i + j][3] = aCellColor[i][3];
                    aBorderTmp[i + j][0] = aBorder[i][0];
                    aBorderTmp[i + j][1] = aBorder[i][1];
                    aAlignmentTmp[i + j] = aAlignment[i];
                } else {
                    j--;
                }

            screenList.setListData(aList);
            if (aList.length > 0) {
                screenList.setCellRenderer(new MyCellRenderer<>(screenList, aCellColorTmp, aBorderTmp, aAlignmentTmp));
            }
            getRenderer(screenList);
        }
    }

    private void fireCounter() {
        this.counterListeners.forEach(c -> c.updateCount(this.formulas.size()));
    }

    private static int getSize(final JList<String> list) {
        ListModel<String> model = list.getModel();
        return model.getSize();
    }

    private static void setAlignment(final JList<String> list, final int index, final int alignment) {
        int len = getSize(list);
        int i = index != -1 ? index : len - 1;
        if (i >= 0 && i < len) {
            getRenderer(list);
            aAlignment[i] = alignment;
            list.setCellRenderer(new MyCellRenderer<>(list, aCellColor, aBorder, aAlignment));
        }
    }

    private static void setBorderColor(final JList<String> list, final int index, final Color border, final Color borderSelected) {
        int len = getSize(list);
        int i = index != -1 ? index : len - 1;
        if (i >= 0 && i < len) {
            getRenderer(list);
            aBorder[i][0] = BorderFactory.createLineBorder(border);
            aBorder[i][1] = BorderFactory.createLineBorder(borderSelected);
            list.setCellRenderer(new MyCellRenderer<>(list, aCellColor, aBorder, aAlignment));
        }
    }

    private static void setColor(final JList<String> list, final int index, final Color background, final Color front,
            final Color backgroundSelected, final Color frontSelected) {
        int len = getSize(list);
        int i = index != -1 ? index : len - 1;
        if (i >= 0 && i < len) {
            getRenderer(list);
            aCellColor[i][0] = background;
            aCellColor[i][1] = front;
            aCellColor[i][2] = backgroundSelected;
            aCellColor[i][3] = frontSelected;
            list.setCellRenderer(new MyCellRenderer<>(list, aCellColor, aBorder, aAlignment));
        }
    }

    private static void getRenderer(final JList<String> list) {
        ListModel<String> model = list.getModel();
        int len = model.getSize();
        aCellColor = new Color[len][4];
        aBorder = new Border[len][2];
        aAlignment = new int[len];
        ListCellRenderer<? super String> render = list.getCellRenderer();

        for (int i = 0; i < len; i++) {
            JLabel labelList = (JLabel) render.getListCellRendererComponent(list, model.getElementAt(i), i, false, true);
            aCellColor[i][0] = labelList.getBackground();
            aCellColor[i][1] = labelList.getForeground();
            aBorder[i][0] = labelList.getBorder();
            aAlignment[i] = labelList.getHorizontalAlignment();

            labelList = (JLabel) render.getListCellRendererComponent(list, model.getElementAt(i), i, true, true);
            aCellColor[i][2] = labelList.getBackground();
            aCellColor[i][3] = labelList.getForeground();
            aBorder[i][1] = labelList.getBorder();
            aAlignment[i] = labelList.getHorizontalAlignment();
        }
    }

    static class MyCellRenderer<T> extends JLabel implements ListCellRenderer<T> {

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 2708791466950985625L;

        private Color aCellColor[][];
        private Border aBorder[][];
        private int aAlignment[];

        public MyCellRenderer(final JList<T> list, final Color aCellColor[][], final Border aBorder[][], final int aAlignment[]) {
            this.aAlignment = new int[aAlignment.length];
            this.aAlignment = aAlignment;
            this.aCellColor = new Color[aCellColor.length][aCellColor[0].length];
            this.aCellColor = aCellColor;
            this.aBorder = new Border[aBorder.length][aBorder[0].length];
            this.aBorder = aBorder;
            setOpaque(true);
        }

        public Component getListCellRendererComponent(final JList<? extends T> list, final T value, final int index,
                final boolean isSelected, boolean cellHasFocus) {
            setText(value.toString());
            if (index < aCellColor.length) {
                setBackground(isSelected ? aCellColor[index][2] : aCellColor[index][0]);
                setForeground(isSelected ? aCellColor[index][3] : aCellColor[index][1]);
                setBorder(isSelected ? aBorder[index][1] : aBorder[index][0]);
                setHorizontalAlignment(aAlignment[index]);
            }
            return this;
        }
    }

    public interface CounterListener {
        void updateCount(Integer count);
    }
}