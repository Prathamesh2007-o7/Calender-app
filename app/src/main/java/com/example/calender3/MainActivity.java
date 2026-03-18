package com.example.calender3;

// res/java/com/example/calendarapp/MainActivity.java

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────
    private TextView     tvMonthYear;
    private TextView     tvSelectedDate;
    private TextView     tvSelectedSub;
    private TextView     tvNotePreview;
    private TextView     btnAddNote;
    private TextView     btnToday;
    private View         noteDivider;
    private LinearLayout noteRow;
    private LinearLayout calendarGrid;
    private ImageButton  btnPrev, btnNext;

    // ── State ──────────────────────────────────────────────────────
    private Calendar displayedMonth;
    private Calendar today;
    private Calendar selectedDay;

    // ── Storage ────────────────────────────────────────────────────
    private SharedPreferences prefs;
    private static final String PREFS_NAME   = "CalendarNotes";
    private static final int    MAX_NOTE_LEN = 200;

    // ── Colours ────────────────────────────────────────────────────
    private int colorAccent;
    private int colorTextDark;
    private int colorTextMuted;
    private int colorOtherMonth;
    private int colorSunday;

    // ══════════════════════════════════════════════════════════════
    //  onCreate
    // ══════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Resolve colours
        colorAccent     = ContextCompat.getColor(this, R.color.accent_coral);
        colorTextDark   = ContextCompat.getColor(this, R.color.text_dark);
        colorTextMuted  = ContextCompat.getColor(this, R.color.text_muted);
        colorOtherMonth = ContextCompat.getColor(this, R.color.text_other_month);
        colorSunday     = ContextCompat.getColor(this, R.color.text_sunday);

        // Bind views
        tvMonthYear    = findViewById(R.id.tvMonthYear);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedSub  = findViewById(R.id.tvSelectedSub);
        tvNotePreview  = findViewById(R.id.tvNotePreview);
        btnAddNote     = findViewById(R.id.btnAddNote);
        btnToday       = findViewById(R.id.btnToday);
        noteDivider    = findViewById(R.id.noteDivider);
        noteRow        = findViewById(R.id.noteRow);
        calendarGrid   = findViewById(R.id.calendarGrid);
        btnPrev        = findViewById(R.id.btnPrev);
        btnNext        = findViewById(R.id.btnNext);

        // Initialise calendars
        today          = Calendar.getInstance();
        displayedMonth = Calendar.getInstance();
        normaliseToFirstOfMonth(displayedMonth);
        selectedDay    = null;

        // ── Navigation ──────────────────────────────────────────────
        btnPrev.setOnClickListener(v -> {
            animateNav(v, -1);
            displayedMonth.add(Calendar.MONTH, -1);
            renderCalendar();
        });

        btnNext.setOnClickListener(v -> {
            animateNav(v, 1);
            displayedMonth.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        btnToday.setOnClickListener(v -> {
            displayedMonth = Calendar.getInstance();
            normaliseToFirstOfMonth(displayedMonth);
            selectedDay    = Calendar.getInstance();
            renderCalendar();
            updateSelectedCard();
        });

        // ── Note button ─────────────────────────────────────────────
        btnAddNote.setOnClickListener(v -> {
            if (selectedDay != null) openNoteDialog(selectedDay);
        });

        renderCalendar();
    }

    // ══════════════════════════════════════════════════════════════
    //  RENDER CALENDAR GRID
    // ══════════════════════════════════════════════════════════════

    private void renderCalendar() {
        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(fmt.format(displayedMonth.getTime()));

        calendarGrid.removeAllViews();

        Calendar cursor = (Calendar) displayedMonth.clone();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = cursor.get(Calendar.DAY_OF_WEEK);
        cursor.add(Calendar.DAY_OF_MONTH, -(firstDow - 1));

        int displayedMonthValue = displayedMonth.get(Calendar.MONTH);
        int displayedYearValue  = displayedMonth.get(Calendar.YEAR);

        for (int row = 0; row < 6; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);

            for (int col = 0; col < 7; col++) {
                final Calendar dayCal      = (Calendar) cursor.clone();
                int  dayNum                = cursor.get(Calendar.DAY_OF_MONTH);
                int  monthNum              = cursor.get(Calendar.MONTH);
                int  yearNum               = cursor.get(Calendar.YEAR);
                boolean isCurrentMonth     = (monthNum == displayedMonthValue && yearNum == displayedYearValue);
                boolean isToday            = isSameDay(cursor, today);
                boolean isSelected         = selectedDay != null && isSameDay(cursor, selectedDay);
                boolean hasNote            = isCurrentMonth && noteExists(cursor);

                // Each cell is a FrameLayout: day number + optional note dot
                FrameLayout cellFrame = buildDayCellFrame(
                        dayNum, col, isCurrentMonth, isToday, isSelected, hasNote);

                cellFrame.setOnClickListener(v -> {
                    selectedDay = dayCal;
                    if (dayCal.get(Calendar.MONTH) != displayedMonth.get(Calendar.MONTH)
                            || dayCal.get(Calendar.YEAR) != displayedMonth.get(Calendar.YEAR)) {
                        displayedMonth = (Calendar) dayCal.clone();
                        normaliseToFirstOfMonth(displayedMonth);
                    }
                    animateCell(v);
                    renderCalendar();
                    updateSelectedCard();
                });

                rowLayout.addView(cellFrame);
                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }

            calendarGrid.addView(rowLayout);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  BUILD DAY CELL (FrameLayout with number + note dot)
    // ══════════════════════════════════════════════════════════════

    private FrameLayout buildDayCellFrame(int dayNum, int col, boolean isCurrentMonth,
                                          boolean isToday, boolean isSelected, boolean hasNote) {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(4, 4, 4, 4);
        frame.setLayoutParams(lp);

        // ── Day number TextView ──────────────────────────────────────
        TextView cell = new TextView(this);
        cell.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        cell.setText(String.valueOf(dayNum));
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(15f);

        if (isSelected) {
            cell.setBackground(ContextCompat.getDrawable(this, R.drawable.cell_selected));
            cell.setTextColor(0xFFFFFFFF);
            cell.setTypeface(null, Typeface.BOLD);
        } else if (isToday) {
            cell.setBackground(ContextCompat.getDrawable(this, R.drawable.cell_today));
            cell.setTextColor(colorAccent);
            cell.setTypeface(null, Typeface.BOLD);
        } else if (isCurrentMonth) {
            cell.setBackground(ContextCompat.getDrawable(this, R.drawable.cell_normal));
            cell.setTextColor(col == 0 ? colorSunday : colorTextDark);
            cell.setTypeface(null, Typeface.NORMAL);
        } else {
            cell.setBackground(null);
            cell.setTextColor(colorOtherMonth);
            cell.setTypeface(null, Typeface.NORMAL);
        }

        frame.addView(cell);

        // ── Note dot indicator (small coral dot at bottom-centre) ────
        if (hasNote) {
            View dot = new View(this);
            int dotSizePx = dpToPx(5);
            FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dotSizePx, dotSizePx);
            dotLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            dotLp.bottomMargin = dpToPx(4);
            dot.setLayoutParams(dotLp);
            // White dot on selected cell, coral dot otherwise
            dot.setBackground(ContextCompat.getDrawable(this,
                    isSelected ? R.drawable.dot_white : R.drawable.dot_coral));
            frame.addView(dot);
        }

        return frame;
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE BOTTOM SELECTED CARD
    // ══════════════════════════════════════════════════════════════

    private void updateSelectedCard() {
        if (selectedDay == null) {
            tvSelectedDate.setText("Select a date");
            tvSelectedSub.setText("Tap any day to highlight it");
            noteDivider.setVisibility(View.GONE);
            noteRow.setVisibility(View.GONE);
            return;
        }

        // Date label
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        tvSelectedDate.setText(dateFmt.format(selectedDay.getTime()));

        // Sub label
        if (isSameDay(selectedDay, today)) {
            tvSelectedSub.setText("That's today! \uD83C\uDF1F");
        } else {
            long diffMs   = selectedDay.getTimeInMillis() - today.getTimeInMillis();
            long diffDays = diffMs / (1000L * 60 * 60 * 24);
            if (diffDays > 0) {
                tvSelectedSub.setText(diffDays + " day" + (diffDays == 1 ? "" : "s") + " from today");
            } else {
                tvSelectedSub.setText(Math.abs(diffDays) + " day" + (Math.abs(diffDays) == 1 ? "" : "s") + " ago");
            }
        }

        // Note row
        noteDivider.setVisibility(View.VISIBLE);
        noteRow.setVisibility(View.VISIBLE);

        String existingNote = getNote(selectedDay);
        if (existingNote != null && !existingNote.isEmpty()) {
            tvNotePreview.setText(existingNote);
            tvNotePreview.setTextColor(colorTextDark);
            btnAddNote.setText("Edit note");
        } else {
            tvNotePreview.setText("Add a note…");
            tvNotePreview.setTextColor(colorTextMuted);
            btnAddNote.setText("Add note");
        }

        // Animate card in
        LinearLayout card = findViewById(R.id.selectedCard);
        card.setAlpha(0f);
        card.setTranslationY(12f);
        card.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    // ══════════════════════════════════════════════════════════════
    //  NOTE DIALOG
    // ══════════════════════════════════════════════════════════════

    private void openNoteDialog(Calendar day) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_note);

        // Make dialog background transparent so our custom bg_drawable shows
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        // Bind dialog views
        TextView  tvDialogDate  = dialog.findViewById(R.id.tvDialogDate);
        EditText  etNote        = dialog.findViewById(R.id.etNote);
        TextView  tvCharCount   = dialog.findViewById(R.id.tvCharCount);
        TextView  btnSave       = dialog.findViewById(R.id.btnSaveNote);
        TextView  btnCancel     = dialog.findViewById(R.id.btnCancelNote);
        TextView  btnDelete     = dialog.findViewById(R.id.btnDeleteNote);

        // Set hint color programmatically (android:hintTextColor is not valid in XML layouts)
        etNote.setHintTextColor(ContextCompat.getColor(this, R.color.text_other_month));

        // Set date label in dialog
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        tvDialogDate.setText(fmt.format(day.getTime()));

        // Pre-fill existing note
        String existing = getNote(day);
        if (existing != null && !existing.isEmpty()) {
            etNote.setText(existing);
            etNote.setSelection(existing.length());
            tvCharCount.setText(existing.length() + " / " + MAX_NOTE_LEN);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvCharCount.setText("0 / " + MAX_NOTE_LEN);
            btnDelete.setVisibility(View.GONE);
        }

        // Max length filter
        etNote.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(MAX_NOTE_LEN) });

        // Character counter
        etNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                int len = s.length();
                tvCharCount.setText(len + " / " + MAX_NOTE_LEN);
                tvCharCount.setTextColor(len >= MAX_NOTE_LEN
                        ? colorAccent : colorTextMuted);
            }
        });

        // Save
        btnSave.setOnClickListener(v -> {
            String noteText = etNote.getText().toString().trim();
            saveNote(day, noteText);
            dialog.dismiss();
            updateSelectedCard();
            renderCalendar();       // refresh dots on grid
            String msg = noteText.isEmpty() ? "Note cleared" : "Note saved";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Cancel
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Delete
        btnDelete.setOnClickListener(v -> {
            deleteNote(day);
            dialog.dismiss();
            updateSelectedCard();
            renderCalendar();
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════
    //  NOTE STORAGE  (SharedPreferences — key = "yyyy-MM-dd")
    // ══════════════════════════════════════════════════════════════

    /** Stable key for a given day. */
    private String noteKey(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    private String getNote(Calendar cal) {
        return prefs.getString(noteKey(cal), "");
    }

    private boolean noteExists(Calendar cal) {
        String note = prefs.getString(noteKey(cal), "");
        return note != null && !note.isEmpty();
    }

    private void saveNote(Calendar cal, String text) {
        prefs.edit().putString(noteKey(cal), text).apply();
    }

    private void deleteNote(Calendar cal) {
        prefs.edit().remove(noteKey(cal)).apply();
    }

    // ══════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════════════════════════

    private void animateCell(View v) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 0.85f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 0.85f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy);
        set.setDuration(180);
        set.start();
    }

    private void animateNav(View v, int direction) {
        float from = direction < 0 ? -6f : 6f;
        ObjectAnimator.ofFloat(v, "translationX", from, 0f).setDuration(140);
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "translationX", from, 0f);
        anim.setDuration(140);
        anim.start();
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
                && a.get(Calendar.MONTH)    == b.get(Calendar.MONTH)
                && a.get(Calendar.YEAR)     == b.get(Calendar.YEAR);
    }

    private void normaliseToFirstOfMonth(Calendar cal) {
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
