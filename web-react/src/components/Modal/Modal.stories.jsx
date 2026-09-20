import { useState } from "react";
import { Modal } from "./Modal";
import { Button } from "../Button/Button";

export default {
  title: "Components/Modal",
  component: Modal,
};

function Demo() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button labelKey="exercises.addButton" variant="secondary" onClick={() => setOpen(true)} />
      <Modal open={open} onClose={() => setOpen(false)} titleKey="exercises.newTitle">
        <p style={{ color: "var(--text)", fontFamily: "var(--font-body)", margin: 0 }}>
          Modal body content goes here.
        </p>
      </Modal>
    </>
  );
}

export const Default = { render: () => <Demo /> };