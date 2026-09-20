import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Toggle } from "./Toggle";

export default {
  title: "Components/Toggle",
  component: Toggle,
};

function SexDemo() {
  const { t } = useTranslation();
  const [value, setValue] = useState("M");
  const options = [
    { value: "M", label: t("profile.sexMale") },
    { value: "W", label: t("profile.sexFemale") },
    { value: "NB", label: t("profile.sexNonBinary") },
  ];
  return <Toggle labelKey="profile.sex" value={value} onChange={setValue} options={options} />;
}

function UnitsDemo() {
  const { t } = useTranslation();
  const [value, setValue] = useState("metric");
  const options = [
    { value: "metric", label: t("profile.metric") },
    { value: "imperial", label: t("profile.imperial") },
  ];
  return <Toggle labelKey="profile.units" value={value} onChange={setValue} options={options} />;
}

export const Sex = { render: () => <SexDemo /> };
export const Units = { render: () => <UnitsDemo /> };