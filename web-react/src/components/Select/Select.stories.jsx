import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Select } from "./Select";
import { MUSCLE_GROUPS } from "../../constants/muscles";

export default {
  title: "Components/Select",
  component: Select,
};

function MuscleDemo() {
  const { t } = useTranslation();
  const [value, setValue] = useState("Legs");
  const options = MUSCLE_GROUPS.map((m) => ({ value: m, label: t(`muscles.${m}`) }));
  return <Select labelKey="exercises.fieldMuscle" value={value} onChange={setValue} options={options} />;
}

export const MuscleGroups = { render: () => <MuscleDemo /> };