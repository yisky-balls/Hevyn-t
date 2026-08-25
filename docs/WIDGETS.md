# Workout widgets

Hevyn’t provides compact 2×1 and main 4×2 workout widgets using classic
`RemoteViews`. Both derive state from the existing Room workout history,
`ActiveWorkoutRepository`, and `RestTimerManager`. The launcher renders rest
countdowns with `Chronometer`; the app does not poll once per second.

## Future TODO — Weekly Training Summary (do not implement in this patch)

Candidate 4×2 widget:

```text
THIS WEEK

4 workouts
68 working sets
18,420 kg volume

M  T  W  T  F  S  S
●     ●  ●        ●
```

Potential additions:

- workout count
- weekly working sets and volume
- days trained with a compact Monday-first activity visualization
- possibly the most-trained muscle group when the data model can support it

Keep this local-first and derive it from Room history rather than creating a
widget-only analytics store.