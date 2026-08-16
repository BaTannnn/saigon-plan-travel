export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiProblem = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  fieldErrors?: ApiFieldError[];
};
