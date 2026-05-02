/*
 * This file provides reusable UI behavior for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';

// Defines rich text editor so related behavior stays grouped in one place.
export function RichTextEditor({ value, onChange }) {
  return (
    <div className="rounded-xl border border-slate-200 transition-colors dark:border-slate-700">
      <ReactQuill
        theme="snow"
        value={value}
        onChange={onChange}
        className="[&_.ql-container]:min-h-[200px] [&_.ql-container]:rounded-b-xl [&_.ql-toolbar]:rounded-t-xl"
      />
    </div>
  );
}
